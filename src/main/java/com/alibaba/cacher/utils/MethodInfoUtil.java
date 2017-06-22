package com.alibaba.cacher.utils;

import com.alibaba.cacher.CacheKey;
import com.alibaba.cacher.Cached;
import com.alibaba.cacher.CachedGet;
import com.alibaba.cacher.Invalid;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.CacheMethodHolder;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.enums.Expire;
import com.alibaba.cacher.exception.CacherException;
import com.google.common.base.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 定位: 将@Cached、@Invalid、@CachedGet、(@CachedPut未来)以及将@CacheKey整体融合到一起
 *
 * @author jifang
 * @since 16/7/20 上午11:49.
 */
public class MethodInfoUtil {

    private static final ConcurrentMap<Method, Pair<CacheKeyHolder, CacheMethodHolder>> cacheMap = new ConcurrentHashMap<>();

    public static Pair<CacheKeyHolder, CacheMethodHolder> getMethodInfo(Method method) {
        return cacheMap.computeIfAbsent(method, MethodInfoUtil::supplier);
    }

    private static Pair<CacheKeyHolder, CacheMethodHolder> supplier(Method method) {
        CacheKeyHolder cacheKeyHolder = supplyKeyHolder(method);
        CacheMethodHolder cacheMethodHolder = supplyMethodHolder(method, cacheKeyHolder);

        return Pair.of(cacheKeyHolder, cacheMethodHolder);
    }

    /****
     * cache key supplier
     ****/

    private static CacheKeyHolder supplyKeyHolder(Method method) {

        CacheKeyHolder.Builder builder = CacheKeyHolder.Builder.newBuilder();

        Annotation[][] pAnnotations = method.getParameterAnnotations();
        scanKeys(builder, pAnnotations);

        if (method.isAnnotationPresent(Cached.class)) {
            scanCached(builder, method.getAnnotation(Cached.class));
        } else if (method.isAnnotationPresent(CachedGet.class)) {
            scanCachedGet(builder, method.getAnnotation(CachedGet.class));
        } else {
            scanInvalid(builder, method.getAnnotation(Invalid.class));
        }

        return builder.build();
    }

    private static CacheKeyHolder.Builder scanKeys(CacheKeyHolder.Builder builder, Annotation[][] pAnnotations) {
        int multiIndex = -1;
        String id = "";
        Map<Integer, CacheKey> cacheKeyMap = new LinkedHashMap<>(pAnnotations.length);

        for (int pIndex = 0; pIndex < pAnnotations.length; ++pIndex) {

            Annotation[] annotations = pAnnotations[pIndex];
            for (Annotation annotation : annotations) {
                if (annotation instanceof CacheKey) {
                    CacheKey cacheKey = (CacheKey) annotation;
                    cacheKeyMap.put(pIndex, cacheKey);
                    if (cacheKey.multi()) {
                        multiIndex = pIndex;
                        id = cacheKey.id();
                    }
                }
            }
        }

        return builder
                .setCacheKeyMap(cacheKeyMap)
                .setMultiIndex(multiIndex)
                .setId(id);
    }

    private static CacheKeyHolder.Builder scanCached(CacheKeyHolder.Builder builder, Cached cached) {
        return builder
                .setCache(cached.cache())
                .setPrefix(cached.prefix())
                .setExpire(cached.expire())
                .setSeparator(cached.separator());
    }

    private static CacheKeyHolder.Builder scanCachedGet(CacheKeyHolder.Builder builder, CachedGet cachedGet) {
        return builder
                .setCache(cachedGet.cache())
                .setPrefix(cachedGet.prefix())
                .setExpire(Expire.NO)
                .setSeparator(cachedGet.separator());
    }

    private static CacheKeyHolder.Builder scanInvalid(CacheKeyHolder.Builder builder, Invalid invalid) {
        return builder
                .setCache(invalid.cache())
                .setPrefix(invalid.prefix())
                .setExpire(Expire.NO)
                .setSeparator(invalid.separator());
    }

    /***
     * cache method supplier
     ***/

    private static CacheMethodHolder supplyMethodHolder(Method method, CacheKeyHolder cacheKeyHolder) {
        boolean isCollectionReturn = Collection.class.isAssignableFrom(method.getReturnType());

        staticAnalyze(method.getParameterTypes(), cacheKeyHolder, isCollectionReturn);

        return new CacheMethodHolder(isCollectionReturn);
    }

    private static void staticAnalyze(Class<?>[] pTypes, CacheKeyHolder cacheKeyHolder, boolean isCollectionReturn) {
        if (isInvalidParam(pTypes, cacheKeyHolder)) {
            throw new CacherException("cache need at least one param key");
        } else if (isInvalidMultiCount(cacheKeyHolder.getCacheKeyMap())) {
            throw new CacherException("only one multi key");
        } else {
            cacheKeyHolder.getCacheKeyMap().forEach((keyIndex, cacheKey) -> {
                if (cacheKey.multi() && isInvalidMulti(pTypes[keyIndex])) {
                    throw new CacherException("multi need a collection instance param");
                }

                if (cacheKey.multi() && isInvalidResult(isCollectionReturn, cacheKey.id())) {
                    throw new CacherException("multi cache && collection method return need a result id");
                }

                if (isInvalidIdentifier(isCollectionReturn, cacheKey.id())) {
                    throw new CacherException("id method a collection return method");
                }
            });
        }
    }

    private static boolean isInvalidParam(Class<?>[] pTypes, CacheKeyHolder cacheKeyHolder) {
        Map<Integer, CacheKey> cacheKeyMap = cacheKeyHolder.getCacheKeyMap();
        String prefix = cacheKeyHolder.getPrefix();

        return (pTypes == null
                || pTypes.length == 0
                || cacheKeyMap.isEmpty())
                && Strings.isNullOrEmpty(prefix);
    }

    private static boolean isInvalidMultiCount(Map<Integer, CacheKey> keyMap) {
        int multiCount = 0;
        for (CacheKey cacheKey : keyMap.values()) {
            if (cacheKey.multi()) {
                ++multiCount;
                if (multiCount > 1) {
                    break;
                }
            }
        }

        return multiCount > 1;
    }

    private static boolean isInvalidIdentifier(boolean isCollectionReturn, String identifier) {
        return !Strings.isNullOrEmpty(identifier) && !isCollectionReturn;
    }

    private static boolean isInvalidResult(boolean isCollectionReturn, String id) {
        return isCollectionReturn && Strings.isNullOrEmpty(id);
    }

    private static boolean isInvalidMulti(Class<?> paramType) {
        return !Collection.class.isAssignableFrom(paramType);
    }
}
