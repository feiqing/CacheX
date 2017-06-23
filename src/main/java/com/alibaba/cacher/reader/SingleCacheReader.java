package com.alibaba.cacher.reader;

import com.alibaba.cacher.ShootingMXBean;
import com.alibaba.cacher.ioc.Inject;
import com.alibaba.cacher.ioc.Singleton;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.CacheMethodHolder;
import com.alibaba.cacher.invoker.Invoker;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.supplier.PatternSupplier;
import com.alibaba.cacher.utils.KeyGenerators;

/**
 * @author jifang
 * @since 2016/11/5 下午3:10.
 */
@Singleton
public class SingleCacheReader extends AbstractCacheReader {

    @Inject
    private CacheManager cacheManager;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder, Invoker invoker, boolean needWrite) throws Throwable {
        String key = KeyGenerators.generateSingleKey(cacheKeyHolder, invoker.getArgs());
        Object result = cacheManager.readSingle(cacheKeyHolder.getCache(), key);

        doRecord(result, key, cacheKeyHolder);
        // not hit
        if (result == null) {
            // write cache
            result = doLogInvoke(invoker::proceed);

            // @since 1.5.4 为了兼容@CachedGet注解, 客户端缓存
            if (needWrite) {
                cacheManager.writeSingle(cacheKeyHolder.getCache(), key, result, cacheKeyHolder.getExpire());
            }
        }

        return result;
    }

    private void doRecord(Object result, String key, CacheKeyHolder cacheKeyHolder) {
        LOGGER.info("single cache hit rate: {}/1, key: {}", result == null ? 0 : 1, key);
        if (this.shootingMXBean != null) {
            String pattern = PatternSupplier.getPattern(cacheKeyHolder);

            if (result != null) {
                this.shootingMXBean.hitIncr(pattern, 1);
            }
            this.shootingMXBean.requireIncr(pattern, 1);
        }
    }
}
