package com.alibaba.cacher.domain;

import com.alibaba.cacher.CacheKey;

import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/29 下午10:43.
 */
public class CacheKeyHolder {

    private String prefix;

    private Map<Integer, CacheKey> cacheKeyMap;

    private int multiIndex = -1;

    private String identifier;

    private CacheKeyHolder(String prefix, Map<Integer, CacheKey> cacheKeyMap, int multiIndex, String identifier) {
        this.prefix = prefix;
        this.cacheKeyMap = cacheKeyMap;
        this.multiIndex = multiIndex;
        this.identifier = identifier;
    }


    public String getPrefix() {
        return prefix;
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

    public String getIdentifier() {
        return identifier;
    }

    public static class Builder {

        private String prefix;

        private Map<Integer, CacheKey> cacheKeyMap;

        private int multiIndex = -1;

        private String identifier;

        public static Builder newCacheKeyHolderBuilder() {
            return new Builder();
        }

        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder setMultiIndex(int multiIndex) {
            this.multiIndex = multiIndex;
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setCacheKeyMap(Map<Integer, CacheKey> cacheKeyMap) {
            this.cacheKeyMap = cacheKeyMap;
            return this;
        }

        public CacheKeyHolder build() {
            return new CacheKeyHolder(prefix, cacheKeyMap, multiIndex, identifier);
        }
    }
}
