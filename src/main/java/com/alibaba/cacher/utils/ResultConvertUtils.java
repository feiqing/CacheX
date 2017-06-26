package com.alibaba.cacher.utils;

import com.alibaba.cacher.supplier.CollectionSupplier;
import com.alibaba.cacher.supplier.MapSuppliers;

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
        cacheMap.forEach((key, value) -> {
            Object id = keyIdMap.get(key);
            resultMap.put(id, value);
        });

        return MapSuppliers.convertInstanceType(mapType, resultMap);
    }

    public static Collection toCollection(Class<?> collectionType,
                                          Map<String, Object> cacheMap) {

        Collection resultCollection = CollectionSupplier.newInstance(collectionType);
        resultCollection.addAll(cacheMap.values());

        return CollectionSupplier.convertInstanceType(collectionType, resultCollection);
    }
}
