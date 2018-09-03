package com.github.cachex.utils;

import com.github.cachex.core.CacheXConfig;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author jifang
 * @since 2016/11/18 下午3:21.
 */
@SuppressWarnings("unchecked")
public class KVConvertUtils {

    public static Map<String, Object> mapToKeyValue(Map proceedEntryValueMap,
                                                    Set<String> missKeys,
                                                    Map<Object, String> multiEntry2Key,
                                                    CacheXConfig.Switch prevent) {
        Map<String, Object> keyValueMap = new HashMap<>(proceedEntryValueMap.size());
        proceedEntryValueMap.forEach((multiArgEntry, value) -> {
            String key = multiEntry2Key.get(multiArgEntry);
            if (Strings.isNullOrEmpty(key)) {
                return;
            }

            missKeys.remove(key);
            keyValueMap.put(key, value);
        });

        // 触发防击穿逻辑
        if (prevent == CacheXConfig.Switch.ON && !missKeys.isEmpty()) {
            missKeys.forEach(key -> keyValueMap.put(key, PreventObjects.getPreventObject()));
        }

        return keyValueMap;
    }

    public static Map<String, Object> collectionToKeyValue(Collection proceedCollection, String idSpel, Set<String> missKeys, Map<Object, String> id2Key, CacheXConfig.Switch prevent) {
        Map<String, Object> keyValueMap = new HashMap<>(proceedCollection.size());

        for (Object value : proceedCollection) {
            Object id = SpelCalculator.calcSpelWithNoContext(idSpel, value);
            String key = id2Key.get(id);

            if (!Strings.isNullOrEmpty(key)) {
                missKeys.remove(key);
                keyValueMap.put(key, value);
            }
        }

        if (prevent == CacheXConfig.Switch.ON && !missKeys.isEmpty()) {
            missKeys.forEach(key -> keyValueMap.put(key, PreventObjects.getPreventObject()));
        }

        return keyValueMap;
    }
}
