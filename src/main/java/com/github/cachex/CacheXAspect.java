package com.github.cachex;

import com.github.cachex.core.CacheXCore;
import com.github.cachex.core.Config;
import com.github.cachex.invoker.adapter.JoinPointInvokerAdapter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/2 下午2:34.
 */
@Aspect
public class CacheXAspect extends CacheXCore {

    private volatile Map<String, ICache> caches;

    private Config config;

    public CacheXAspect(Map<String, ICache> caches) {
        this(caches, new Config(true, false, null));
    }

    public CacheXAspect(Map<String, ICache> caches, Config config) {
        this.caches = caches;
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }

    @PostConstruct
    public void init()
            throws MalformedObjectNameException,
            NotCompliantMBeanException,
            InstanceAlreadyExistsException,
            MBeanRegistrationException, IOException {
        super.init(caches, config);
    }

    @Around("@annotation(com.github.cachex.CachedGet)")
    public Object read(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        CachedGet cachedGet = method.getAnnotation(CachedGet.class);

        return super.read(config.isOpen(), cachedGet, method, new JoinPointInvokerAdapter(pjp));
    }

    @Around("@annotation(com.github.cachex.Cached)")
    public Object readWrite(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Cached cached = method.getAnnotation(Cached.class);

        return super.readWrite(config.isOpen(), cached, method, new JoinPointInvokerAdapter(pjp));
    }

    @After("@annotation(com.github.cachex.Invalid)")
    public void remove(JoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Invalid invalid = method.getAnnotation(Invalid.class);
        super.remove(config.isOpen(), invalid, method, pjp.getArgs());
    }

    @PreDestroy
    public void tearDown() throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        super.tearDown();
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
