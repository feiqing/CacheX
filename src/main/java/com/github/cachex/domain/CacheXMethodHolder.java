package com.github.cachex.domain;

/**
 * @author jifang
 * @since 2016/11/29 下午10:41.
 */
public class CacheXMethodHolder {

    private Class<?> innerReturnType;

    private Class<?> returnType;

    private boolean collection;

    public CacheXMethodHolder(boolean collection) {
        this.collection = collection;
    }

    public boolean isCollection() {
        return collection;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public Class<?> getInnerReturnType() {
        return innerReturnType;
    }

    public void setInnerReturnType(Class<?> innerReturnType) {
        this.innerReturnType = innerReturnType;
    }
}
