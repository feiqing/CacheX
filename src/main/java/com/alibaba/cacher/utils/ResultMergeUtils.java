package com.alibaba.cacher.utils;

import java.util.*;

/**
 * @author jifang
 * @since 2016/11/18 下午3:43.
 */
@SuppressWarnings("unchecked")
public class ResultMergeUtils {

    public static Map mapMerge(Map<String, Object> keyIdMap, Class<?> returnType,
                               Map idValueMap, Map<String, Object> keyValueMap)
            throws IllegalAccessException, InstantiationException {

        //Arrays.asList()
        Map mergedMap = null;
        try {
            if (returnType == Class.forName("java.util.Collections.EmptyMap")) {
                mergedMap = new HashMap();
            }

                mergedMap = (Map) returnType.newInstance();
        } catch (ClassNotFoundException e) {
        }

        // keep maps order
        for (Map.Entry<String, Object> keyIdEntry : keyIdMap.entrySet()) {
            Object id = keyIdEntry.getValue();

            // get idValueMap first
            Object value = idValueMap.get(id);
            if (value == null) {
                String key = keyIdEntry.getKey();
                value = keyValueMap.get(key);
            }

            mergedMap.put(id, value);
        }

        return mergedMap;
    }

    public static Collection collectionMerge(Set<String> keys, Class<?> returnType,
                                             Map<String, Object> keyValueMap1, Map<String, Object> keyValueMap2)
            throws IllegalAccessException, InstantiationException {

        Collection mergedCollection = (Collection) returnType.newInstance();

        for (String key : keys) {
            Object value = keyValueMap1.get(key);
            if (value == null) {
                value = keyValueMap2.get(key);
            }

            mergedCollection.add(value);
        }

        return mergedCollection;
    }
}
