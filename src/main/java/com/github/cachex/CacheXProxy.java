package com.github.cachex;

import com.github.cachex.core.CacheXCore;
import com.github.cachex.di.CacheXDIContainer;
import com.github.cachex.invoker.adapter.InvocationInvokerAdapter;
import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.ProxyFactory;
import org.apache.commons.proxy.factory.cglib.CglibProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * @author jifang.zjf
 * @since 2017/6/22 上午11:04.
 */
public class CacheXProxy implements FactoryBean, InitializingBean {

    private Object target;

    private Object proxy;

    private Class<?>[] types;

    private Map<String, ICache> caches;

    private CacheXCore cacheXCore;

    private boolean isNeedCGLIB = false;

    private boolean open = true;

    public CacheXProxy(Object target, String cacheName, ICache cache) {
        this(target, Collections.singletonMap(cacheName, cache));
    }

    public CacheXProxy(Object target, Map<String, ICache> caches) {
        this(target, target.getClass().getInterfaces()[0], caches);
    }

    public CacheXProxy(Object target, Class<?> type, Map<String, ICache> caches) {
        this(target, new Class[]{type}, caches, true);
    }

    public CacheXProxy(Object target, Class<?>[] types, Map<String, ICache> caches, boolean open) {
        this.target = target;
        if (types == null || types.length == 0) {
            isNeedCGLIB = true;
            types = new Class<?>[]{target.getClass()};
        }
        this.types = types;
        this.caches = caches;
        this.open = open;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ProxyFactory factory = isNeedCGLIB ? new CglibProxyFactory() : new ProxyFactory();
        this.proxy = factory.createInterceptorProxy(target, interceptor, types);
        this.cacheXCore = CacheXDIContainer.getBeanInstance(CacheXCore.class);
        if (cacheXCore == null || !this.cacheXCore.isInited()) {
            cacheXCore = new CacheXCore();
            cacheXCore.init(caches, null);
        }
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
                result = cacheXCore.readWrite(open, cached, method, new InvocationInvokerAdapter(target, invocation));
            } else if ((cachedGet = method.getAnnotation(CachedGet.class)) != null) {
                result = cacheXCore.read(open, cachedGet, method, new InvocationInvokerAdapter(target, invocation));
            } else if ((invalid = method.getAnnotation(Invalid.class)) != null) {
                cacheXCore.remove(open, invalid, method, invocation.getArguments());
                result = null;
            } else {
                result = invocation.proceed();
            }

            return result;
        }
    };

    @Override
    public Object getObject() throws Exception {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return types[0];
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}