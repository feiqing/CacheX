package com.alibaba.cacher.reader;

import com.alibaba.cacher.Cached;
import com.alibaba.cacher.config.Inject;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.constant.Constant;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.MethodInfoHolder;
import com.alibaba.cacher.jmx.RecordMXBean;
import com.alibaba.cacher.utils.KeyPatternsCombineUtil;
import com.alibaba.cacher.utils.KeysCombineUtil;
import com.alibaba.cacher.manager.CacheManager;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author jifang
 * @since 2016/11/5 下午3:10.
 */
@Singleton
public class SingleCacheReader implements CacheReader {

    @Inject
    private CacheManager cacheManager;

    @Override
    public Object read(CacheKeyHolder holder, Cached cached, ProceedingJoinPoint pjp, MethodInfoHolder ret) throws Throwable {

        String key = KeysCombineUtil.toSingleKey(holder, cached.separator(), pjp.getArgs());
        String keyPattern = KeyPatternsCombineUtil.getKeyPattern(holder, cached.separator());

        Object result = cacheManager.readSingle(cached.cache(), key);

        doRecord(result, key, keyPattern);
        // not hit
        if (result == null) {
            // write cache
            result = pjp.proceed();
            cacheManager.writeSingle(cached.cache(), key, result, cached.expire());
        }

        return result;
    }

    private void doRecord(Object result, String key, String keyPattern) {
        String rate;
        if (result == null) {
            rate = "0/1";
        } else {
            rate = "1/1";
            RecordMXBean.HIT_COUNT_MAP.get(Constant.TOTAL_KEY).incrementAndGet();
            RecordMXBean.HIT_COUNT_MAP.get(keyPattern).incrementAndGet();
        }
        RecordMXBean.REQUIRE_COUNT_MAP.get(Constant.TOTAL_KEY).incrementAndGet();
        RecordMXBean.REQUIRE_COUNT_MAP.get(keyPattern).incrementAndGet();

        LOGGER.info("single cache hit rate: {}, key: {}", rate, key);
    }
}
