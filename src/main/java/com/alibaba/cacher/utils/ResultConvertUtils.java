package com.alibaba.cacher.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午4:34.
 */
@SuppressWarnings("unchecked")
public class ResultConvertUtils {

    public static Map toMap(Class<?> mapType, Map<String, Object> fromCacheMap, Map<String, Object> keyIdMap) {

        Map resultMap = MapSuppliers.newInstance(mapType);
        fromCacheMap.forEach((key, value) -> {
            Object id = keyIdMap.get(key);

            resultMap.put(id, value);
        });

        return MapSuppliers.convertInstanceType(mapType, resultMap);
    }

    public static Collection toCollection(Class<?> returnType, Collection fromCacheCollection) {

        Collection collection = CollectionSupplier.newInstance(returnType);
        collection.addAll(fromCacheCollection);

        return CollectionSupplier.convertInstanceType(returnType, collection);
    }
}
