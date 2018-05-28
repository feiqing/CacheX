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

    private CacheXCore cacheXCore;

    public CacheXProxy(Object target, Map<String, ICache> caches) {
        this(target, (Class<T>) target.getClass().getInterfaces()[0], caches);
    }

    public CacheXProxy(Object target, Class<T> type, Map<String, ICache> caches) {
        this.target = target;
        this.type = type;
        this.proxy = newProxy();
        this.cacheXCore = CacheXModule.coreInstance(CacheXConfig.newConfig(caches));
    }

    private Object newProxy() {
        ProxyFactory factory = this.type.isInterface() ? new ProxyFactory() : new CglibProxyFactory();
        return factory.createInterceptorProxy(target, interceptor, new Class[]{type});
    }

    private Interceptor interceptor = new Interceptor() {

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            Cached cached;
            CachedGet cachedGet;
            Invalid invalid;

            Method method = invocation.getMethod();
            Object result;
            if ((cached = method.getAnnotation(Cached.class)) != null) {
                result = cacheXCore.readWrite(cached, method, new InvocationInvokerAdapter(target, invocation));
            } else if ((cachedGet = method.getAnnotation(CachedGet.class)) != null) {
                result = cacheXCore.read(cachedGet, method, new InvocationInvokerAdapter(target, invocation));
            } else if ((invalid = method.getAnnotation(Invalid.class)) != null) {
                cacheXCore.remove(invalid, method, invocation.getArguments());
                result = null;
            } else {
                result = invocation.proceed();
            }

            return result;
        }
    };

    @Override
    public T getObject() {
        return (T) proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}