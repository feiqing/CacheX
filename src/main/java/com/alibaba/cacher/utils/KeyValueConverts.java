package com.alibaba.cacher.utils;

import com.alibaba.cacher.supplier.SpelValueSupplier;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午3:21.
 */
public class KeyValueConverts {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> idValueToKeyValue(Map id2Value, Map<Object, String> id2Key) {
        Map<String, Object> keyValueMap = new HashMap<>(id2Value.size());

        id2Value.forEach((id, value) -> {
            String key = id2Key.get(id);
            if (!Strings.isNullOrEmpty(key)) {
                keyValueMap.put(key, value);
            }
        });

        return keyValueMap;
    }

    public static Map<String, Object> collectionToKeyValue(Collection proceedCollection, String idSpel, Map<Object, String> id2Key) {
        Map<String, Object> keyValueMap = new HashMap<>(proceedCollection.size());

        for (Object value : proceedCollection) {
            Object id = SpelValueSupplier.calcSpelValue(idSpel, value);
            String key = id2Key.get(id);

            if (!Strings.isNullOrEmpty(key)) {
                keyValueMap.put(key, value);
            }
        }

        return keyValueMap;
    }
}
