package com.github.cachex.utils;

import com.google.common.base.Strings;

import java.util.Map;

/**
 * @author jifang
 * @since 16/7/19 下午4:59.
 */
public class CacheXUtils {

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static StringBuilder appendSeparator(StringBuilder sb, String prefix, int index, String separator) {
        if (!Strings.isNullOrEmpty(separator)) {
            if (!Strings.isNullOrEmpty(prefix) || index != 0) {
                sb.append(separator);
            }
        }

        return sb;
    }
}
