package com.github.cachex.domain;

import com.github.cachex.CacheKey;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/29 下午10:43.
 */
public class CacheKeyHolder {

    private Method method;

    // ******************* //
    // --- cached 内容 ---- //
    // ******************* //
    private String cache;

    private String prefix;

    private int expire;

    private String separator;

    // ****************** //
    // --- @CacheKey --- //
    // ****************** //
    private Map<Integer, CacheKey> cacheKeyMap;

    private int multiIndex = -1;

    private String id;

    private CacheKeyHolder(Method method,
                           String cache, String prefix, int expire, String separator,
                           Map<Integer, CacheKey> cacheKeyMap, int multiIndex, String id) {
        this.method = method;
        this.cache = cache;
        this.prefix = prefix;
        this.expire = expire;
        this.separator = separator;
        this.cacheKeyMap = cacheKeyMap;
        this.multiIndex = multiIndex;
        this.id = id;
    }

    public Method getMethod() {
        return method;
    }

    public String getCache() {
        return cache;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getExpire() {
        return expire;
    }

    public String getSeparator() {
        return separator;
    }

    public Map<Integer, CacheKey> getCacheKeyMap() {
        return cacheKeyMap;
    }

    public int getMultiIndex() {
        return multiIndex;
    }

    public boolean isMulti() {
        return multiIndex != -1;
    }

    public String getId() {
        return id;
    }

    public static class Builder {

        private Method method;

        private String cache;

        private String prefix;

        private int expire;

        private String separator;

        private Map<Integer, CacheKey> cacheKeyMap;

        private int multiIndex = -1;

        private String id;

        private Builder(Method method) {
            this.method = method;
        }

        public static Builder newBuilder(Method method) {
            return new Builder(method);
        }

        public Builder setCache(String cache) {
            this.cache = cache;
            return this;
        }

        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder setExpire(int expire) {
            this.expire = expire;
            return this;
        }

        public Builder setSeparator(String separator) {
            this.separator = separator;
            return this;
        }

        public Builder setMultiIndex(int multiIndex) {
            this.multiIndex = multiIndex;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setCacheKeyMap(Map<Integer, CacheKey> cacheKeyMap) {
            this.cacheKeyMap = cacheKeyMap;
            return this;
        }

        public CacheKeyHolder build() {
            return new CacheKeyHolder(method, cache, prefix, expire, separator, cacheKeyMap, multiIndex, id);
        }
    }
}
