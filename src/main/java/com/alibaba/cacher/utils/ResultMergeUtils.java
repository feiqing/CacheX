package com.alibaba.cacher.utils;

import com.alibaba.cacher.supplier.CollectionSupplier;
import com.alibaba.cacher.supplier.MapSuppliers;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午3:43.
 */
@SuppressWarnings("unchecked")
public class ResultMergeUtils {

    public static Map mergeMap(Map<String, Object> keyIdMap, Class<?> mapType,
                               Map proceedMap,
                               Map<String, Object> cacheMap) {

        Map mergedMap = MapSuppliers.newInstance(mapType, proceedMap);

        cacheMap.forEach((key, value) -> {
            Object id = keyIdMap.get(key);
            mergedMap.put(id, value);
        });

        return MapSuppliers.convertInstanceType(mapType, mergedMap);
    }

    public static Collection mergeCollection(Class<?> collectionType,
                                             Collection proceedCollection,
                                             Map<String, Object> cacheMap) {
        Collection mergedCollection = CollectionSupplier.newInstance(collectionType, proceedCollection);
        mergedCollection.addAll(cacheMap.values());

        return CollectionSupplier.convertInstanceType(collectionType, mergedCollection);
    }
}
