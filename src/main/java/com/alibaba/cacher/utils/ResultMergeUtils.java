package com.alibaba.cacher.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午3:43.
 */
@SuppressWarnings("unchecked")
public class ResultMergeUtils {

    public static Map mapMerge(Map<String, Object> keyIdMap, Class<?> mapType,
                               Map fromMethodMap, Map<String, Object> fromCacheMap) {

        Map mergedMap = MapSuppliers.newInstance(mapType, fromMethodMap);

        fromCacheMap.forEach((key, value) -> {
            Object id = keyIdMap.get(key);
            mergedMap.put(id, value);
        });

        return MapSuppliers.convertInstanceType(mapType, mergedMap);
    }

    public static Collection collectionMerge(Class<?> collectionType, Collection fromMethodCollection,
                                             Map<String, Object> fromCacheMap) {
        Collection collection = CollectionSupplier.newInstance(collectionType, fromMethodCollection);
        collection.addAll(fromCacheMap.values());

        return CollectionSupplier.convertInstanceType(collectionType, collection);
    }

    /*
    public static Collection collectionMerge(Set<String> keys, Class<?> returnType,
                                             Map<String, Object> keyValueMap1, Map<String, Object> keyValueMap2) {

        CollectionSupplier.newInstance(returnType, )

        Collection mergedCollection = (Collection) returnType.newInstance();

        for (String key : keys) {
            Object value = keyValueMap1.get(key);
            if (value == null) {
                value = keyValueMap2.get(key);
            }

            mergedCollection.add(value);
        }

        return mergedCollection;
    }*/
}
