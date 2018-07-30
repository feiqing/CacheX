package com.github.cachex.reader;

import com.github.cachex.ShootingMXBean;
import com.github.cachex.core.CacheXConfig;
import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.domain.CacheMethodHolder;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheManager;
import com.github.cachex.supplier.PatternSupplier;
import com.github.cachex.supplier.PreventObjects;
import com.github.cachex.utils.CacheXLogger;
import com.github.cachex.utils.KeyGenerators;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jifang
 * @since 2016/11/5 下午3:10.
 */
@Singleton
public class SingleCacheReader extends AbstractCacheReader {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private CacheXConfig config;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder, Invoker invoker, boolean needWrite) throws Throwable {
        String key = KeyGenerators.generateSingleKey(cacheKeyHolder, invoker.getArgs());
        Object readResult = cacheManager.readSingle(cacheKeyHolder.getCache(), key);

        doRecord(readResult, key, cacheKeyHolder);
        // 命中
        if (readResult != null) {
            // 是放击穿对象
            if (PreventObjects.isPrevent(readResult)) {
                return null;
            }

            return readResult;
        }


        // not hit
        // invoke method
        Object invokeResult = doLogInvoke(invoker::proceed);
        if (invokeResult != null && cacheMethodHolder.getInnerReturnType() == null) {
            cacheMethodHolder.setInnerReturnType(invokeResult.getClass());
        }

        if (!needWrite) {
            return invokeResult;
        }

        if (invokeResult != null) {
            cacheManager.writeSingle(cacheKeyHolder.getCache(), key, invokeResult, cacheKeyHolder.getExpire());
            return invokeResult;
        }

        // invokeResult is null
        if (config.isPreventOn()) {
            cacheManager.writeSingle(cacheKeyHolder.getCache(), key, PreventObjects.getPreventObject(), cacheKeyHolder.getExpire());
        }

        return null;
    }

    private void doRecord(Object result, String key, CacheKeyHolder cacheKeyHolder) {
        CacheXLogger.CACHEX.info("single cache hit rate: {}/1, key: {}", result == null ? 0 : 1, key);
        if (this.shootingMXBean != null) {
            String pattern = PatternSupplier.getPattern(cacheKeyHolder);

            if (result != null) {
                this.shootingMXBean.hitIncr(pattern, 1);
            }
            this.shootingMXBean.requireIncr(pattern, 1);
        }
    }
}
