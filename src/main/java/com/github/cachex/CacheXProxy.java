package com.github.cachex;

import com.github.cachex.core.CacheXConfig;
import com.github.cachex.core.CacheXCore;
import com.github.cachex.core.CacheXModule;
import com.github.cachex.invoker.adapter.InvocationInvokerAdapter;
import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.ProxyFactory;
import org.apache.commons.proxy.factory.cglib.CglibProxyFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author jifang.zjf
 * @since 2017/6/22 上午11:04.
 */
@SuppressWarnings("unchecked")
public class CacheXProxy<T> implements FactoryBean<T> {

    private Object target;

    private Object proxy;

    private Class<T> type;

    private CacheXConfig.Switch cglib = CacheXConfig.Switch.OFF;

    private CacheXCore cacheXCore;

    public CacheXProxy(Object target, Map<String, ICache> caches) {
        this(target, (Class<T>) target.getClass().getInterfaces()[0], caches, CacheXConfig.Switch.OFF);
    }

    public CacheXProxy(Object target, Class<T> type, Map<String, ICache> caches, CacheXConfig.Switch cglib) {
        this.target = target;
        this.type = type;
        this.cglib = cglib;
        this.proxy = newProxy();
        this.cacheXCore = CacheXModule.coreInstance(CacheXConfig.newConfig(caches));
    }

    private Object newProxy() {
        ProxyFactory factory;
        if (cglib == CacheXConfig.Switch.ON || !this.type.isInterface()) {
            factory = new CglibProxyFactory();
        } else {
            factory = new ProxyFactory();
        }

        return factory.createInterceptorProxy(target, interceptor, new Class[]{type});
    }

    private Interceptor interceptor = new Interceptor() {

        @Override
        public Object intercept(Invocation invocation) throws Throwable {

            Method method = invocation.getMethod();
            Cached cached;
            if ((cached = method.getAnnotation(Cached.class)) != null) {
                return cacheXCore.readWrite(cached, method, new InvocationInvokerAdapter(target, invocation));
            }

            CachedGet cachedGet;
            if ((cachedGet = method.getAnnotation(CachedGet.class)) != null) {
                return cacheXCore.read(cachedGet, method, new InvocationInvokerAdapter(target, invocation));
            }

            Invalid invalid;
            if ((invalid = method.getAnnotation(Invalid.class)) != null) {
                cacheXCore.remove(invalid, method, invocation.getArguments());
                return null;
            }

            return invocation.proceed();
        }
    };

    @Override
    public T getObject() {
        return (T) proxy;
    }

    @Override
    public Class<T> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}