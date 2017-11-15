package com.alibaba.cacher.utils;

import com.alibaba.cacher.supplier.CollectionSupplier;
import com.alibaba.cacher.supplier.MapSuppliers;
import com.alibaba.cacher.supplier.PreventObjectSupplier;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午4:34.
 */
@SuppressWarnings("unchecked")
public class ResultConvertUtils {

    public static Map toMap(Map<String, Object> keyIdMap, Class<?> mapType,
                            Map<String, Object> cacheMap) {

        Map resultMap = MapSuppliers.newInstance(mapType);
        cacheMap.entrySet().stream()
                .filter(entry -> !PreventObjectSupplier.isGeneratePreventObject(entry.getValue()))  //将防击穿Object过滤掉
                .forEach(entry -> {
                    Object id = keyIdMap.get(entry.getKey());
                    resultMap.put(id, entry.getValue());
                });

        return MapSuppliers.convertInstanceType(mapType, resultMap);
    }

    public static Collection toCollection(Class<?> collectionType,
                                          Map<String, Object> cacheMap) {

        Collection resultCollection = CollectionSupplier.newInstance(collectionType);
        cacheMap.values().stream()
                .filter(value -> !PreventObjectSupplier.isGeneratePreventObject(value)) // 将防击穿Object过滤掉
                .forEach(resultCollection::add);

        return CollectionSupplier.convertInstanceType(collectionType, resultCollection);
    }
}
