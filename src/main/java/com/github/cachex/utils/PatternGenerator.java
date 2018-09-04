package com.github.cachex.utils;

import com.github.cachex.CacheKey;
import com.github.cachex.domain.CacheXAnnoHolder;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang
 * @since 2017/3/1 下午5:55.
 */
public class PatternGenerator {

    private static final ConcurrentMap<Method, String> patterns = new ConcurrentHashMap<>();

    public static String generatePattern(CacheXAnnoHolder cacheXAnnoHolder) {
        return patterns.computeIfAbsent(cacheXAnnoHolder.getMethod(), (method) -> doPatternCombiner(cacheXAnnoHolder));
    }

    private static String doPatternCombiner(CacheXAnnoHolder cacheXAnnoHolder) {
        StringBuilder sb = new StringBuilder(cacheXAnnoHolder.getPrefix());
        Collection<CacheKey> cacheKeys = cacheXAnnoHolder.getCacheKeyMap().values();
        for (CacheKey cacheKey : cacheKeys) {
            sb.append(cacheKey.value());
        }

        return sb.toString();
    }
}
