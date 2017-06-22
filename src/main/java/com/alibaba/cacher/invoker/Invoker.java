package com.alibaba.cacher.invoker;

/**
 * @author jifang.zjf
 * @since 2017/6/22 下午4:22.
 */
public interface Invoker {

    Object[] getArgs();

    Object proceed() throws Throwable;

    Object proceed(Object[] args) throws Throwable;
}
