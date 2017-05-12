package com.alibaba.cacher.utils;

import com.google.common.base.Strings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午3:21.
 */
public class KeyValueConverts {

    public static Map<String, Object> idValueMap2KeyValue(Map idValueMap, Map<Object, String> idKeyMap) {
        Map<String, Object> keyValueMap = new HashMap<>(idValueMap.size());

        for (Object entry : idValueMap.entrySet()) {
            Map.Entry mapEntry = (Map.Entry) entry;

            String key = idKeyMap.get(mapEntry.getKey());
            if (!Strings.isNullOrEmpty(key)) {
                Object value = mapEntry.getValue();
                keyValueMap.put(key, value);
            }
        }

        return keyValueMap;
    }

    public static Map<String, Object> collection2KeyValue(Collection collection, String identifier, Map<Object, String> idKeyMap) {
        Map<String, Object> keyValueMap = new HashMap<>(collection.size());

        for (Object value : collection) {
            Object id = CacherUtils.getExpressionValue(identifier, value);
            String key = idKeyMap.get(id);

            if (!Strings.isNullOrEmpty(key)) {
                keyValueMap.put(key, value);
            }
        }

        return keyValueMap;
    }
}
