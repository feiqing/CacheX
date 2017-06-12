package com.alibaba.cacher.reader;

import com.alibaba.cacher.Cached;
import com.alibaba.cacher.config.Inject;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.MethodInfoHolder;
import com.alibaba.cacher.shooting.ShootingMXBean;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.utils.KeyPatternsCombineUtil;
import com.alibaba.cacher.utils.KeysCombineUtil;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author jifang
 * @since 2016/11/5 下午3:10.
 */
@Singleton
public class SingleCacheReader implements CacheReader {

    @Inject
    private CacheManager cacheManager;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheKeyHolder holder, Cached cached, ProceedingJoinPoint pjp, MethodInfoHolder ret) throws Throwable {

        String key = KeysCombineUtil.toSingleKey(holder, cached.separator(), pjp.getArgs());
        String keyPattern = KeyPatternsCombineUtil.getKeyPattern(holder, cached.separator());

        Object result = cacheManager.readSingle(cached.cache(), key);

        doRecord(result, key, keyPattern);
        // not shooting
        if (result == null) {
            // write cache
            result = pjp.proceed();
            cacheManager.writeSingle(cached.cache(), key, result, cached.expire());
        }

        return result;
    }

    private void doRecord(Object result, String key, String keyPattern) {
        if (this.shootingMXBean != null) {
            String rate;
            if (result == null) {
                rate = "0/1";
            } else {
                rate = "1/1";
                this.shootingMXBean.hitIncr(keyPattern, 1);
            }
            this.shootingMXBean.requireIncr(keyPattern, 1);

            LOGGER.info("single cache hit shooting: {}, key: {}", rate, key);
        }
    }
}
