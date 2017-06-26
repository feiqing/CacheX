package com.alibaba.cacher.invoker.adapter;

import com.alibaba.cacher.invoker.Invoker;
import org.apache.commons.proxy.Invocation;

/**
 * @author jifang.zjf
 * @since 2017/6/26 下午4:04.
 */
public class InvocationInvokerAdapter implements Invoker {

    private Object target;

    private Invocation invocation;

    public InvocationInvokerAdapter(Object target, Invocation invocation) {
        this.target = target;
        this.invocation = invocation;
    }

    @Override
    public Object[] getArgs() {
        return invocation.getArguments();
    }

    @Override
    public Object proceed() throws Throwable {
        return invocation.proceed();
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
        return invocation.getMethod().invoke(target, args);
    }
}
