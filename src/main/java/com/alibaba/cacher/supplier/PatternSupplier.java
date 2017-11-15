package com.alibaba.cacher.supplier;

import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.utils.CacherUtils;

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
        // -> "keyExp"

        cacheKeyHolder.getCacheKeyMap().forEach((index, cacheKey) -> {
            // append key separator (like : "-")
            CacherUtils.appendSeparator(sb, cacheKeyHolder.getPrefix(), index, cacheKeyHolder.getSeparator());
            // -> "keyExp-"

            // append key keyExp (like: "id:")
            sb.append(cacheKey.prefix());
            // -> "keyExp-id:"

            // append placeholder (like: "[*]")
            sb.append(PATTERN_PLACEHOLDER);
            // -> "keyExp-id:[*]"
        });

        return sb.toString();
    }
}
