package com.github.cachex.supplier;

import com.github.cachex.domain.CacheKeyHolder;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang
 * @since 2017/3/1 下午5:55.
 */
public class PatternSupplier {

    private static final String PATTERN_PLACEHOLDER = "[*]";

    private static final ConcurrentMap<Method, String> patterns = new ConcurrentHashMap<>();

    public static String getPattern(CacheKeyHolder cacheKeyHolder) {
        return patterns.computeIfAbsent(cacheKeyHolder.getMethod(), (method) -> doPatternCombiner(cacheKeyHolder));
    }

    private static String doPatternCombiner(CacheKeyHolder cacheKeyHolder) {
        StringBuilder sb = new StringBuilder(cacheKeyHolder.getPrefix());
        cacheKeyHolder.getCacheKeyMap().forEach((index, cacheKey) -> {
            sb.append(cacheKey.prefix());
            sb.append(PATTERN_PLACEHOLDER);
        });

        return sb.toString();
    }
}
