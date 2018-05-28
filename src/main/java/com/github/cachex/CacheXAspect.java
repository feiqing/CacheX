package com.github.cachex;

import com.github.cachex.core.CacheXConfig;
import com.github.cachex.core.CacheXCore;
import com.github.cachex.core.CacheXModule;
import com.github.cachex.invoker.adapter.JoinPointInvokerAdapter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/2 下午2:34.
 */
@Aspect
public class CacheXAspect {

    private CacheXCore core;

    public CacheXAspect(Map<String, ICache> caches) {
        this(new CacheXConfig(caches));
    }

    public CacheXAspect(CacheXConfig config) {
        core = CacheXModule.coreInstance(config);
    }

    @Around("@annotation(com.github.cachex.CachedGet)")
    public Object read(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        CachedGet cachedGet = method.getAnnotation(CachedGet.class);
        return core.read(cachedGet, method, new JoinPointInvokerAdapter(pjp));
    }

    @Around("@annotation(com.github.cachex.Cached)")
    public Object readWrite(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Cached cached = method.getAnnotation(Cached.class);

        return core.readWrite(cached, method, new JoinPointInvokerAdapter(pjp));
    }

    @After("@annotation(com.github.cachex.Invalid)")
    public void remove(JoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Invalid invalid = method.getAnnotation(Invalid.class);
        core.remove(invalid, method, pjp.getArgs());
    }

    private Method getMethod(JoinPoint pjp) throws NoSuchMethodException {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = pjp.getTarget().getClass().getDeclaredMethod(ms.getName(), method.getParameterTypes());
        }

        return method;
    }
}
