package com.github.cachex.core;

import com.github.cachex.Cached;
import com.github.cachex.CachedGet;
import com.github.cachex.Invalid;
import com.github.cachex.domain.CacheXAnnoHolder;
import com.github.cachex.domain.CacheXMethodHolder;
import com.github.cachex.domain.Pair;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheManager;
import com.github.cachex.reader.AbstractCacheReader;
import com.github.cachex.utils.CacheXInfoContainer;
import com.github.cachex.utils.CacheXLogger;
import com.github.cachex.utils.KeyGenerator;
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

            long start = System.currentTimeMillis();

            CacheXAnnoHolder cacheXAnnoHolder = CacheXInfoContainer.getCacheXInfo(method).getLeft();
            if (cacheXAnnoHolder.isMulti()) {
                Map[] pair = KeyGenerator.generateMultiKey(cacheXAnnoHolder, args);
                Set<String> keys = ((Map<String, Object>) pair[1]).keySet();
                cacheManager.remove(invalid.value(), keys.toArray(new String[keys.size()]));

                CacheXLogger.info("multi cache clear, keys: {}", keys);
            } else {
                String key = KeyGenerator.generateSingleKey(cacheXAnnoHolder, args);
                cacheManager.remove(invalid.value(), key);

                CacheXLogger.info("single cache clear, key: {}", key);
            }

            CacheXLogger.debug("cachex clear total cost [{}] ms", (System.currentTimeMillis() - start));
        }
    }

    private Object doReadWrite(Method method, Invoker invoker, boolean needWrite) throws Throwable {
        long start = System.currentTimeMillis();

        Pair<CacheXAnnoHolder, CacheXMethodHolder> pair = CacheXInfoContainer.getCacheXInfo(method);
        CacheXAnnoHolder cacheXAnnoHolder = pair.getLeft();
        CacheXMethodHolder cacheXMethodHolder = pair.getRight();

        Object result;
        if (cacheXAnnoHolder.isMulti()) {
            result = multiCacheReader.read(cacheXAnnoHolder, cacheXMethodHolder, invoker, needWrite);
        } else {
            result = singleCacheReader.read(cacheXAnnoHolder, cacheXMethodHolder, invoker, needWrite);
        }

        CacheXLogger.debug("cachex read total cost [{}] ms", (System.currentTimeMillis() - start));

        return result;
    }

    public void write() {
        // TODO on @CachedPut
    }
}
