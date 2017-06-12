package com.alibaba.cacher.utils;

import com.alibaba.cacher.CacheKey;
import com.alibaba.cacher.Cached;
import com.alibaba.cacher.Invalid;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.MethodInfoHolder;
import com.alibaba.cacher.domain.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 16/7/20 上午11:49.
 */
public class MethodInfoUtil {

    private static final Map<Method, Pair<CacheKeyHolder, MethodInfoHolder>> ruleRetCache = new ConcurrentHashMap<>();

    public static Pair<CacheKeyHolder, MethodInfoHolder> getMethodInfo(Method method) {
        Pair<CacheKeyHolder, MethodInfoHolder> pair = ruleRetCache.get(method);
        if (pair == null) {
            CacheKeyHolder cacheKeyHolder = getCacheKeyHolder(method);
            boolean isCollectionReturn = Collection.class.isAssignableFrom(method.getReturnType());

            MethodUtils.staticAnalyze(method.getParameterTypes(), cacheKeyHolder, isCollectionReturn);

            MethodInfoHolder ret = new MethodInfoHolder(isCollectionReturn);

            pair = new Pair<>(cacheKeyHolder, ret);
            ruleRetCache.put(method, pair);
        }

        return pair;
    }

    private static CacheKeyHolder getCacheKeyHolder(Method method) {

        CacheKeyHolder.Builder builder = CacheKeyHolder.Builder.newCacheKeyHolderBuilder();

        Annotation[][] pAnnotations = method.getParameterAnnotations();
        scanCacheKeys(builder, pAnnotations);

        if (method.isAnnotationPresent(Cached.class)) {
            scanCached(builder, method.getAnnotation(Cached.class));
        } else {
            scanCached(builder, method.getAnnotation(Invalid.class));
        }

        return builder.build();
    }


    private static CacheKeyHolder.Builder scanCached(CacheKeyHolder.Builder builder, Cached cached) {
        return builder.setPrefix(cached.prefix());
    }

    private static CacheKeyHolder.Builder scanCached(CacheKeyHolder.Builder builder, Invalid invalid) {
        return builder.setPrefix(invalid.prefix());
    }

    private static CacheKeyHolder.Builder scanCacheKeys(CacheKeyHolder.Builder builder, Annotation[][] pAnnotations) {
        int multiIndex = -1;
        String identifier = "";
        Map<Integer, CacheKey> cacheKeyMap = new LinkedHashMap<>(pAnnotations.length);

        for (int pIndex = 0; pIndex < pAnnotations.length; ++pIndex) {

            Annotation[] annotations = pAnnotations[pIndex];
            for (Annotation annotation : annotations) {
                if (annotation instanceof CacheKey) {
                    CacheKey cacheKey = (CacheKey) annotation;
                    cacheKeyMap.put(pIndex, cacheKey);
                    if (cacheKey.multi()) {
                        multiIndex = pIndex;
                        identifier = cacheKey.identifier();
                    }
                }
            }
        }

        return builder
                .setCacheKeyMap(cacheKeyMap)
                .setMultiIndex(multiIndex)
                .setIdentifier(identifier);
    }
}
