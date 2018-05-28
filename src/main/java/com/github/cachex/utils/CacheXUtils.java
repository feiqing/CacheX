package com.github.cachex.utils;

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
}
