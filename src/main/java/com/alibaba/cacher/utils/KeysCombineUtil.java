package com.alibaba.cacher.utils;

import com.google.common.base.Strings;
import com.alibaba.cacher.CacheKey;
import com.alibaba.cacher.domain.CacheKeyHolder;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 16/7/21 上午11:34.
 */
public class KeysCombineUtil {

    public static String toSingleKey(CacheKeyHolder holder, String separator, Object[] args) {
        String prefix = holder.getPrefix();
        Map<Integer, CacheKey> cacheKeyMap = holder.getCacheKeyMap();

        StringBuilder sb = new StringBuilder(prefix);
        for (Map.Entry<Integer, CacheKey> entry : cacheKeyMap.entrySet()) {
            int index = entry.getKey();
            CacheKey cacheKey = entry.getValue();

            // append key separator (like : "-")
            if (!Strings.isNullOrEmpty(separator) &&
                    (!Strings.isNullOrEmpty(prefix) || index != 0)) {
                sb.append(separator);
            }

            // append key prefix (like: "user: xxx")
            sb.append(cacheKey.prefix());

            // append true arg value (like: "123")
            Object argValue = CacherUtils.getExpressionValue(cacheKey.spel(), args[index]);
            sb.append(argValue);
        }

        return sb.toString();
    }

    //{idKeyMap, keyIdMap}
    public static Map[] toMultiKey(CacheKeyHolder holder, String separator, Object[] args) {
        String prefix = holder.getPrefix();
        Map<Integer, CacheKey> cacheKeyMap = holder.getCacheKeyMap();
        int multiIndex = holder.getMultiIndex();
        Collection multiArgValues = (Collection) args[multiIndex];

        Map<Object, String> idKeyMap = new LinkedHashMap<>();
        Map<String, Object> keyIdMap = new LinkedHashMap<>();
        if (multiArgValues != null) {
            for (Object multiArgVal : multiArgValues) {

                StringBuilder sb = new StringBuilder(prefix);
                for (Map.Entry<Integer, CacheKey> entry : cacheKeyMap.entrySet()) {
                    int index = entry.getKey();
                    CacheKey cacheKey = entry.getValue();

                    // append key separator
                    if (!Strings.isNullOrEmpty(separator) &&
                            (!Strings.isNullOrEmpty(prefix) || index != 0)) {
                        sb.append(separator);
                    }

                    // append key prefix
                    sb.append(cacheKey.prefix());

                    // append true arg value
                    Object argValue = (index == multiIndex ? multiArgVal : args[index]);
                    argValue = CacherUtils.getExpressionValue(cacheKey.spel(), argValue);
                    sb.append(argValue);
                }

                String key = sb.toString();

                idKeyMap.put(multiArgVal, key);
                keyIdMap.put(key, multiArgVal);
            }
        }

        return new Map[]{idKeyMap, keyIdMap};
    }
}
