package com.github.cachex.utils;

import com.github.cachex.supplier.CollectionSupplier;
import com.github.cachex.supplier.MapSuppliers;
import com.github.cachex.supplier.PreventObjectSupplier;

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
        cacheMap.entrySet().stream()
                .filter(entry -> !PreventObjectSupplier.isGeneratePreventObject(entry.getValue()))  // 将防击穿Object过滤掉
                .forEach(entry -> {
                    // 将key转换为id
                    Object id = keyIdMap.get(entry.getKey());
                    mergedMap.put(id, entry.getValue());
                });

        return MapSuppliers.convertInstanceType(mapType, mergedMap);
    }

    public static Collection mergeCollection(Class<?> collectionType,
                                             Collection proceedCollection,
                                             Map<String, Object> cacheMap) {
        Collection mergedCollection = CollectionSupplier.newInstance(collectionType, proceedCollection);
        cacheMap.values().stream()
                .filter(value -> !PreventObjectSupplier.isGeneratePreventObject(value)) // 将防击穿Object过滤掉
                .forEach(mergedCollection::add);

        return CollectionSupplier.convertInstanceType(collectionType, mergedCollection);
    }
}
