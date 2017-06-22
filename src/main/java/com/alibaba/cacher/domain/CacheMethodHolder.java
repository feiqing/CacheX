package com.alibaba.cacher.domain;

/**
 * @author jifang
 * @since 2016/11/29 下午10:41.
 */
public class CacheMethodHolder {

    private Class<?> type;

    private boolean collection;

    public CacheMethodHolder(boolean collection) {
        this.collection = collection;
    }

    public boolean isCollection() {
        return collection;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
}
