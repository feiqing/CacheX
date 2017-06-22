package com.alibaba.cacher.utils;

import com.alibaba.cacher.domain.CacheKeyHolder;
import com.google.common.base.Strings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang
 * @since 2017/3/1 下午5:55.
 */
public class PatternSupplier {

    private static final String PATTERN_PLACEHOLDER = "[*]";

    private static final ConcurrentMap<CacheKeyHolder, String> patterns = new ConcurrentHashMap<>();

    public static String getPattern(CacheKeyHolder cacheKeyHolder) {
        return patterns.computeIfAbsent(cacheKeyHolder, PatternSupplier::combiner);
    }

    private static String combiner(CacheKeyHolder holder) {
        StringBuilder sb = new StringBuilder(holder.getPrefix());
        // -> "prefix"

        holder.getCacheKeyMap().forEach((index, cacheKey) -> {
            // append key separator (like : "-")
            appendSeparator(sb, holder.getPrefix(), index, holder.getSeparator());
            // -> "prefix-"

            // append key prefix (like: "id:")
            sb.append(cacheKey.prefix());
            // -> "prefix-id:"

            // append placeholder (like: "[*]")
            sb.append(PATTERN_PLACEHOLDER);
            // -> "prefix-id:[*]"
        });

        return sb.toString();
    }

    private static StringBuilder appendSeparator(StringBuilder sb, String totalPrefix, int index, String separator) {
        if (!Strings.isNullOrEmpty(separator)) {
            if (!Strings.isNullOrEmpty(totalPrefix) || index != 0) {
                sb.append(separator);
            }
        }

        return sb;
    }
}
