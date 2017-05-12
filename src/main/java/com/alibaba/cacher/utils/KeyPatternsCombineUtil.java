package com.alibaba.cacher.utils;

import com.alibaba.cacher.CacheKey;
import com.google.common.base.Strings;
import com.alibaba.cacher.domain.CacheKeyHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 2017/3/1 下午5:55.
 */
public class KeyPatternsCombineUtil {

    private static final Map<HolderWithSeparatorMapKey, String> cache = new ConcurrentHashMap<>();

    private static final String CACHE_KEY_PLACEHOLDER = "[ ]";

    public static String getKeyPattern(CacheKeyHolder holder, String separator) {
        HolderWithSeparatorMapKey key = new HolderWithSeparatorMapKey(holder, separator);
        String pattern = cache.get(key);
        if (Strings.isNullOrEmpty(pattern)) {
            // process
            pattern = combineKeyPattern(holder, separator);
            cache.put(key, pattern);
        }

        return pattern;
    }

    private static String combineKeyPattern(CacheKeyHolder holder, String separator) {
        String prefix = holder.getPrefix();
        Map<Integer, CacheKey> cacheKeyMap = holder.getCacheKeyMap();

        StringBuilder sb = new StringBuilder(prefix);
        for (Map.Entry<Integer, CacheKey> entry : cacheKeyMap.entrySet()) {
            int index = entry.getKey();
            CacherUtils.appendSeparator(sb, separator, prefix, index);

            // append key prefix (like: "user: xxx")
            sb.append(entry.getValue().prefix());

            // append true arg value (like: "123")
            sb.append(CACHE_KEY_PLACEHOLDER);
        }

        return sb.toString();
    }

    private static class HolderWithSeparatorMapKey {
        private CacheKeyHolder holder;

        private String separator;

        public HolderWithSeparatorMapKey(CacheKeyHolder holder, String separator) {
            this.holder = holder;
            this.separator = separator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HolderWithSeparatorMapKey)) return false;

            HolderWithSeparatorMapKey that = (HolderWithSeparatorMapKey) o;

            // compare holder
            if (this.holder != null) {
                // use "==": holder used cache
                if (!this.holder.equals(that.holder)) {
                    return false;
                }
            } else {
                if (that.holder != null) {
                    return false;
                }
            }

            // compare separator
            if (this.separator != null) {
                return this.separator.equals(that.separator);
            } else {
                return that.separator == null;
            }

        }

        @Override
        public int hashCode() {
            int result = holder != null ? holder.hashCode() : 0;
            result = 31 * result + (separator != null ? separator.hashCode() : 0);
            return result;
        }
    }
}
