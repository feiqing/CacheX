package com.alibaba.cacher.utils;

import com.alibaba.cacher.exception.CacherException;
import com.google.common.base.Strings;
import com.alibaba.cacher.CacheKey;
import com.alibaba.cacher.domain.CacheKeyHolder;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/30 上午10:47.
 */
public class MethodUtils {

    public static void staticAnalyze(Class<?>[] pTypes, CacheKeyHolder cacheKeyHolder, boolean isCollectionReturn) {
        if (isInvalidParam(pTypes, cacheKeyHolder)) {
            throw new CacherException("cache need at least one param key");
        } else if (isInvalidMultiCount(cacheKeyHolder.getCacheKeyMap())) {
            throw new CacherException("only one multi key");
        } else {
            for (Map.Entry<Integer, CacheKey> entry : cacheKeyHolder.getCacheKeyMap().entrySet()) {
                int keyIndex = entry.getKey();
                CacheKey cacheKey = entry.getValue();

                if (cacheKey.multi() && isInvalidMulti(pTypes[keyIndex])) {
                    throw new CacherException("multi need a collection instance param");
                }

                if (cacheKey.multi() && isInvalidResult(isCollectionReturn, cacheKey.id())) {
                    throw new CacherException("multi cache && collection method return need a result id");
                }

                if (isInvalidIdentifier(isCollectionReturn, cacheKey.id())) {
                    throw new CacherException("id method a collection return method");
                }
            }
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

    private static boolean isInvalidResult(boolean isCollectionReturn, String identifier) {
        return isCollectionReturn && Strings.isNullOrEmpty(identifier);
    }

    private static boolean isInvalidMulti(Class<?> paramType) {
        return !Collection.class.isAssignableFrom(paramType);
    }
}
