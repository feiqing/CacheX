package com.github.cachex.core;

import com.github.cachex.Cached;
import com.github.cachex.CachedGet;
import com.github.cachex.Invalid;
import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.domain.CacheMethodHolder;
import com.github.cachex.domain.Pair;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheManager;
import com.github.cachex.reader.AbstractCacheReader;
import com.github.cachex.supplier.CacheXInfoSupplier;
import com.github.cachex.utils.CacheXLogger;
import com.github.cachex.utils.KeyGenerators;
import com.github.cachex.utils.SwitcherUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.github.cachex.utils.SwitcherUtils.isSwitchOn;

/**
 * @author jifang.zjf
 * @since 2017/6/26 下午1:03.
 */
@Singleton
public class CacheXCore {

    @Inject
    private CacheXConfig config;

    @Inject
    private CacheManager cacheManager;

    @Inject
    @Named("singleCacheReader")
    private AbstractCacheReader singleCacheReader;

    @Inject
    @Named("multiCacheReader")
    private AbstractCacheReader multiCacheReader;

    public Object read(CachedGet cachedGet, Method method, Invoker invoker) throws Throwable {
        Object result;
        if (isSwitchOn(config, cachedGet, method, invoker.getArgs())) {
            result = doReadWrite(method, invoker, false);
        } else {
            result = invoker.proceed();
        }

        return result;
    }

    public Object readWrite(Cached cached, Method method, Invoker invoker) throws Throwable {
        Object result;
        if (isSwitchOn(config, cached, method, invoker.getArgs())) {
            result = doReadWrite(method, invoker, true);
        } else {
            result = invoker.proceed();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public void remove(Invalid invalid, Method method, Object[] args) {
        if (isSwitchOn(config, invalid, method, args)) {

            long start = 0;
            if (CacheXLogger.CACHEX.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }

            CacheKeyHolder cacheKeyHolder = CacheXInfoSupplier.getMethodInfo(method).getLeft();
            if (cacheKeyHolder.isMulti()) {
                Map[] keyIdPair = KeyGenerators.generateMultiKey(cacheKeyHolder, args);
                Set<String> keys = ((Map<String, Object>) keyIdPair[1]).keySet();
                cacheManager.remove(invalid.cache(), keys.toArray(new String[keys.size()]));

                CacheXLogger.CACHEX.info("multi cache clear, keys: {}", keys);
            } else {
                String key = KeyGenerators.generateSingleKey(cacheKeyHolder, args);
                cacheManager.remove(invalid.cache(), key);

                CacheXLogger.CACHEX.info("single cache clear, key: {}", key);
            }

            if (CacheXLogger.CACHEX.isDebugEnabled()) {
                CacheXLogger.CACHEX.info("cachex clear total cost [{}] ms", (System.currentTimeMillis() - start));
            }
        }
    }

    private Object doReadWrite(Method method, Invoker invoker, boolean needWrite) throws Throwable {
        long start = 0;
        if (CacheXLogger.CACHEX.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }

        Pair<CacheKeyHolder, CacheMethodHolder> pair = CacheXInfoSupplier.getMethodInfo(method);
        CacheKeyHolder cacheKeyHolder = pair.getLeft();
        CacheMethodHolder cacheMethodHolder = pair.getRight();

        Object result;
        if (cacheKeyHolder.isMulti()) {
            result = multiCacheReader.read(cacheKeyHolder, cacheMethodHolder, invoker, needWrite);
        } else {
            result = singleCacheReader.read(cacheKeyHolder, cacheMethodHolder, invoker, needWrite);
        }

        if (CacheXLogger.CACHEX.isDebugEnabled()) {
            CacheXLogger.CACHEX.debug("cachex read total cost [{}] ms", (System.currentTimeMillis() - start));
        }

        return result;
    }

    public void write() {
        // TODO on @CachedPut
    }
}
