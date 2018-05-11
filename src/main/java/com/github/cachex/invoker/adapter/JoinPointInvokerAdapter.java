package com.github.cachex.invoker.adapter;

import com.github.cachex.invoker.Invoker;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author jifang.zjf
 * @since 2017/6/22 下午4:26.
 */
public class JoinPointInvokerAdapter implements Invoker {

    private ProceedingJoinPoint proceedingJoinPoint;

    public JoinPointInvokerAdapter(ProceedingJoinPoint proceedingJoinPoint) {
        this.proceedingJoinPoint = proceedingJoinPoint;
    }

    @Override
    public Object[] getArgs() {
        return proceedingJoinPoint.getArgs();
    }

    @Override
    public Object proceed() throws Throwable {
        return proceedingJoinPoint.proceed();
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
        return proceedingJoinPoint.proceed(args);
    }
}
