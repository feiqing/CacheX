package com.alibaba.cacher;

import com.alibaba.cacher.core.CacherCore;
import com.alibaba.cacher.invoker.JoinPointInvokerAdapter;
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
public class CacherAspect extends CacherCore {

    private volatile boolean open;

    private ShootingMXBean shootingMXBean;

    private volatile Map<String, ICache> caches;

    public CacherAspect(Map<String, ICache> caches) {
        this(caches, true);
    }

    public CacherAspect(Map<String, ICache> caches, boolean open) {
        this(caches, open, null);
    }

    public CacherAspect(Map<String, ICache> caches, boolean open, ShootingMXBean shootingMXBean) {
        this.caches = caches;
        this.open = open;
        this.shootingMXBean = shootingMXBean;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    @PostConstruct
    public void init()
            throws MalformedObjectNameException,
            NotCompliantMBeanException,
            InstanceAlreadyExistsException,
            MBeanRegistrationException, IOException {
        super.init(caches, shootingMXBean);
    }

    @Around("@annotation(com.alibaba.cacher.CachedGet)")
    public Object read(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        CachedGet cachedGet = method.getAnnotation(CachedGet.class);

        return super.read(isOpen(), cachedGet, method, new JoinPointInvokerAdapter(pjp));
    }

    @Around("@annotation(com.alibaba.cacher.Cached)")
    public Object readWrite(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Cached cached = method.getAnnotation(Cached.class);

        return super.readWrite(open, cached, method, new JoinPointInvokerAdapter(pjp));
    }

    @After("@annotation(com.alibaba.cacher.Invalid)")
    public void remove(JoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Invalid invalid = method.getAnnotation(Invalid.class);
        super.doRemove(open, invalid, method, pjp.getArgs());
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
