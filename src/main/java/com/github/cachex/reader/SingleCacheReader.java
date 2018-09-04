package com.github.cachex.reader;

import com.github.cachex.ShootingMXBean;
import com.github.cachex.core.CacheXConfig;
import com.github.cachex.domain.CacheXAnnoHolder;
import com.github.cachex.domain.CacheXMethodHolder;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheManager;
import com.github.cachex.utils.PatternGenerator;
import com.github.cachex.utils.PreventObjects;
import com.github.cachex.utils.CacheXLogger;
import com.github.cachex.utils.KeyGenerator;
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
    public Object read(CacheXAnnoHolder cacheXAnnoHolder, CacheXMethodHolder cacheXMethodHolder, Invoker invoker, boolean needWrite) throws Throwable {
        String key = KeyGenerator.generateSingleKey(cacheXAnnoHolder, invoker.getArgs());
        Object readResult = cacheManager.readSingle(cacheXAnnoHolder.getCache(), key);

        doRecord(readResult, key, cacheXAnnoHolder);
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
        if (invokeResult != null && cacheXMethodHolder.getInnerReturnType() == null) {
            cacheXMethodHolder.setInnerReturnType(invokeResult.getClass());
        }

        if (!needWrite) {
            return invokeResult;
        }

        if (invokeResult != null) {
            cacheManager.writeSingle(cacheXAnnoHolder.getCache(), key, invokeResult, cacheXAnnoHolder.getExpire());
            return invokeResult;
        }

        // invokeResult is null
        if (config.isPreventOn()) {
            cacheManager.writeSingle(cacheXAnnoHolder.getCache(), key, PreventObjects.getPreventObject(), cacheXAnnoHolder.getExpire());
        }

        return null;
    }

    private void doRecord(Object result, String key, CacheXAnnoHolder cacheXAnnoHolder) {
        CacheXLogger.info("single cache hit rate: {}/1, key: {}", result == null ? 0 : 1, key);
        if (this.shootingMXBean != null) {
            String pattern = PatternGenerator.generatePattern(cacheXAnnoHolder);

            if (result != null) {
                this.shootingMXBean.hitIncr(pattern, 1);
            }
            this.shootingMXBean.reqIncr(pattern, 1);
        }
    }
}
