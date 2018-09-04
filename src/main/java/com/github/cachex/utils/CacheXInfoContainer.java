package com.github.cachex.utils;

import com.github.cachex.CacheKey;
import com.github.cachex.Cached;
import com.github.cachex.CachedGet;
import com.github.cachex.Invalid;
import com.github.cachex.domain.CacheXAnnoHolder;
import com.github.cachex.domain.CacheXMethodHolder;
import com.github.cachex.domain.Pair;
import com.github.cachex.enums.Expire;
import com.github.cachex.exception.CacheXException;
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
public class CacheXInfoContainer {

    private static final ConcurrentMap<Method, Pair<CacheXAnnoHolder, CacheXMethodHolder>> cacheMap = new ConcurrentHashMap<>();

    public static Pair<CacheXAnnoHolder, CacheXMethodHolder> getCacheXInfo(Method method) {
        return cacheMap.computeIfAbsent(method, CacheXInfoContainer::doGetMethodInfo);
    }

    private static Pair<CacheXAnnoHolder, CacheXMethodHolder> doGetMethodInfo(Method method) {
        CacheXAnnoHolder cacheXAnnoHolder = getAnnoHolder(method);
        CacheXMethodHolder cacheXMethodHolder = getMethodHolder(method, cacheXAnnoHolder);

        return Pair.of(cacheXAnnoHolder, cacheXMethodHolder);
    }

    /****
     * cache key doGetMethodInfo
     ****/

    private static CacheXAnnoHolder getAnnoHolder(Method method) {

        CacheXAnnoHolder.Builder builder = CacheXAnnoHolder.Builder.newBuilder(method);

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

    private static CacheXAnnoHolder.Builder scanKeys(CacheXAnnoHolder.Builder builder, Annotation[][] pAnnotations) {
        int multiIndex = -1;
        String id = "";
        Map<Integer, CacheKey> cacheKeyMap = new LinkedHashMap<>(pAnnotations.length);

        for (int pIndex = 0; pIndex < pAnnotations.length; ++pIndex) {

            Annotation[] annotations = pAnnotations[pIndex];
            for (Annotation annotation : annotations) {
                if (annotation instanceof CacheKey) {
                    CacheKey cacheKey = (CacheKey) annotation;
                    cacheKeyMap.put(pIndex, cacheKey);
                    if (isMulti(cacheKey)) {
                        multiIndex = pIndex;
                        id = cacheKey.field();
                    }
                }
            }
        }

        return builder
                .setCacheKeyMap(cacheKeyMap)
                .setMultiIndex(multiIndex)
                .setId(id);
    }

    private static CacheXAnnoHolder.Builder scanCached(CacheXAnnoHolder.Builder builder, Cached cached) {
        return builder
                .setCache(cached.value())
                .setPrefix(cached.prefix())
                .setExpire(cached.expire());
    }

    private static CacheXAnnoHolder.Builder scanCachedGet(CacheXAnnoHolder.Builder builder, CachedGet cachedGet) {
        return builder
                .setCache(cachedGet.value())
                .setPrefix(cachedGet.prefix())
                .setExpire(Expire.NO);
    }

    private static CacheXAnnoHolder.Builder scanInvalid(CacheXAnnoHolder.Builder builder, Invalid invalid) {
        return builder
                .setCache(invalid.value())
                .setPrefix(invalid.prefix())
                .setExpire(Expire.NO);
    }

    /***
     * cache method doGetMethodInfo
     ***/

    private static CacheXMethodHolder getMethodHolder(Method method, CacheXAnnoHolder cacheXAnnoHolder) {
        boolean isCollectionReturn = Collection.class.isAssignableFrom(method.getReturnType());
        boolean isMapReturn = Map.class.isAssignableFrom(method.getReturnType());

        staticAnalyze(method.getParameterTypes(),
                cacheXAnnoHolder,
                isCollectionReturn,
                isMapReturn);

        return new CacheXMethodHolder(isCollectionReturn);
    }

    private static void staticAnalyze(Class<?>[] pTypes, CacheXAnnoHolder cacheXAnnoHolder,
                                      boolean isCollectionReturn, boolean isMapReturn) {
        if (isInvalidParam(pTypes, cacheXAnnoHolder)) {
            throw new CacheXException("cache need at least one param key");
        } else if (isInvalidMultiCount(cacheXAnnoHolder.getCacheKeyMap())) {
            throw new CacheXException("only one multi key");
        } else {
            Map<Integer, CacheKey> cacheKeyMap = cacheXAnnoHolder.getCacheKeyMap();
            for (Map.Entry<Integer, CacheKey> entry : cacheKeyMap.entrySet()) {
                Integer argIndex = entry.getKey();
                CacheKey cacheKey = entry.getValue();

                if (isMulti(cacheKey) && isInvalidMulti(pTypes[argIndex])) {
                    throw new CacheXException("multi need a collection instance param");
                }

                if (isMulti(cacheKey) && isInvalidResult(isCollectionReturn, cacheKey.field())) {
                    throw new CacheXException("multi cache && collection method return need a result field");
                }

                if (isInvalidIdentifier(isMapReturn, isCollectionReturn, cacheKey.field())) {
                    throw new CacheXException("id method a collection return method");
                }
            }
        }
    }

    private static boolean isMulti(CacheKey cacheKey) {
        if (cacheKey == null) {
            return false;
        }

        String value = cacheKey.value();
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        return value.contains("#i");
    }

    private static boolean isInvalidParam(Class<?>[] pTypes, CacheXAnnoHolder cacheXAnnoHolder) {
        Map<Integer, CacheKey> cacheKeyMap = cacheXAnnoHolder.getCacheKeyMap();
        String prefix = cacheXAnnoHolder.getPrefix();

        return (pTypes == null
                || pTypes.length == 0
                || cacheKeyMap.isEmpty())
                && Strings.isNullOrEmpty(prefix);
    }

    private static boolean isInvalidMultiCount(Map<Integer, CacheKey> keyMap) {
        int multiCount = 0;
        for (CacheKey cacheKey : keyMap.values()) {
            if (isMulti(cacheKey)) {
                ++multiCount;
                if (multiCount > 1) {
                    break;
                }
            }
        }

        return multiCount > 1;
    }

    private static boolean isInvalidIdentifier(boolean isMapReturn,
                                               boolean isCollectionReturn,
                                               String field) {
        if (isMapReturn && !Strings.isNullOrEmpty(field)) {

            CacheXLogger.warn("@CacheKey's 'field = \"{}\"' is useless.", field);

            return false;
        }

        return !Strings.isNullOrEmpty(field) && !isCollectionReturn;
    }

    private static boolean isInvalidResult(boolean isCollectionReturn, String id) {
        return isCollectionReturn && Strings.isNullOrEmpty(id);
    }

    private static boolean isInvalidMulti(Class<?> paramType) {
        return !Collection.class.isAssignableFrom(paramType)
                && !paramType.isArray();
        // 永久不能放开  && !Map.class.isAssignableFrom(paramType);
    }
}
