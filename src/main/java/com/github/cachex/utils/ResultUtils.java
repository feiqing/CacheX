package com.github.cachex.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午3:43.
 */
public class ResultUtils {

    public static Map mergeMap(Class<?> resultMapType,
                               Map proceedEntryValueMap,
                               Map<String, Object> key2MultiEntry,
                               Map<String, Object> hitKeyValueMap) {

        Map resultMap = Addables.newMap(resultMapType, proceedEntryValueMap);
        mergeCacheValueToResultMap(resultMap, hitKeyValueMap, key2MultiEntry);
        return resultMap;
    }

    public static Map toMap(Class<?> resultMapType,
                            Map<String, Object> key2MultiEntry,
                            Map<String, Object> hitKeyValueMap) {
        Map resultMap = Addables.newMap(resultMapType, null);
        mergeCacheValueToResultMap(resultMap, hitKeyValueMap, key2MultiEntry);
        return resultMap;
    }

    // 将缓存命中的内容都合并到返回值内
    private static void mergeCacheValueToResultMap(Map resultMap,
                                                   Map<String, Object> hitKeyValueMap,
                                                   Map<String, Object> key2MultiEntry) {
        for (Map.Entry<String, Object> entry : hitKeyValueMap.entrySet()) {
            Object inCacheValue = entry.getValue();
            if (PreventObjects.isPrevent(inCacheValue)) {
                continue;
            }

            String cacheKey = entry.getKey();
            Object multiArgEntry = key2MultiEntry.get(cacheKey);

            resultMap.put(multiArgEntry, inCacheValue);
        }
    }


    public static Collection mergeCollection(Class<?> collectionType,
                                             Collection proceedCollection,
                                             Map<String, Object> hitKeyValueMap) {
        Collection resultCollection = Addables.newCollection(collectionType, proceedCollection);
        mergeCacheValueToResultCollection(resultCollection, hitKeyValueMap);
        return resultCollection;
    }

    public static Collection toCollection(Class<?> collectionType,
                                          Map<String, Object> hitKeyValueMap) {

        Collection resultCollection = Addables.newCollection(collectionType, null);

        mergeCacheValueToResultCollection(resultCollection, hitKeyValueMap);

        return resultCollection;
    }

    private static void mergeCacheValueToResultCollection(Collection resultCollection,
                                                          Map<String, Object> hitKeyValueMap) {
        for (Object inCacheValue : hitKeyValueMap.values()) {
            if (PreventObjects.isPrevent(inCacheValue)) {
                continue;
            }

            resultCollection.add(inCacheValue);
        }
    }
}
