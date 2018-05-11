package com.github.cachex.reader;

import com.github.cachex.ShootingMXBean;
import com.github.cachex.core.Config;
import com.github.cachex.di.Inject;
import com.github.cachex.di.Singleton;
import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.domain.CacheMethodHolder;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheXManager;
import com.github.cachex.supplier.PatternSupplier;
import com.github.cachex.supplier.PreventObjectSupplier;
import com.github.cachex.utils.KeyGenerators;

/**
 * @author jifang
 * @since 2016/11/5 下午3:10.
 */
@Singleton
public class SingleCacheReader extends AbstractCacheReader {

    @Inject
    private CacheXManager cacheManager;

    @Inject
    private Config config;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder, Invoker invoker, boolean needWrite) throws Throwable {
        String key = KeyGenerators.generateSingleKey(cacheKeyHolder, invoker.getArgs());
        Object result = cacheManager.readSingle(cacheKeyHolder.getCache(), key);

        doRecord(result, key, cacheKeyHolder);
        // not hit
        if (result == null) {
            // invoke method
            result = doLogInvoke(invoker::proceed);
            if (result != null && cacheMethodHolder.getInnerReturnType() == null) {
                cacheMethodHolder.setInnerReturnType(result.getClass());
            }

            // @since 1.5.4 为了兼容@CachedGet注解, 客户端缓存
            if (needWrite) {

                // 触发缓存防击穿策略
                if (result == null && config.isPreventBreakdown()) {
                    result = PreventObjectSupplier.generatePreventObject();
                }
                cacheManager.writeSingle(cacheKeyHolder.getCache(), key, result, cacheKeyHolder.getExpire());
            }
        }
        // 虽然命中, 但如果是防击穿对象, 则需要将result转换为null返回
        else if (PreventObjectSupplier.isGeneratePreventObject(result)) {
            result = null;
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
