package com.alibaba.cacher;

import com.alibaba.cacher.config.Inject;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.constant.Constant;
import com.alibaba.cacher.domain.MethodInfoHolder;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.jmx.RecordMXBean;
import com.alibaba.cacher.reader.CacheReader;
import com.alibaba.cacher.utils.*;
import com.google.common.base.Preconditions;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.jmx.RecordMXBeanImpl;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.support.cache.NoOpCache;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author jifang
 * @since 2016/11/2 下午2:34.
 */
@Aspect
@Singleton
@SuppressWarnings("unchecked")
public class CacherAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger("com.alibaba.cacher");

    private static final String DEFAULT = "default";

    @Inject
    private CacheReader singleCacheReader;

    @Inject
    private CacheReader multiCacheReader;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private MBeanServer mBeanServer;

    private volatile boolean open;

    private volatile boolean jmxSupport;

    private volatile Map<String, ICache> caches;

    public CacherAspect() {
        this(Collections.singletonMap(DEFAULT, (ICache) new NoOpCache()));
    }

    public CacherAspect(Map<String, ICache> caches) {
        this(caches, true);
    }

    public CacherAspect(Map<String, ICache> caches, boolean open) {
        this(caches, open, true);
    }

    public CacherAspect(Map<String, ICache> caches, boolean open, boolean jmxSupport) {
        this.caches = initCaches(caches);
        this.open = open;
        this.jmxSupport = jmxSupport;
    }

    @PostConstruct
    public void setUp()
            throws MalformedObjectNameException,
            NotCompliantMBeanException,
            InstanceAlreadyExistsException,
            MBeanRegistrationException, IOException {
        CacherInitUtil.beanInit(Constant.CACHER_BASE_PACKAGE, this);
        this.cacheManager.setICachePool(this.caches);

        if (this.jmxSupport) {
            RecordMXBean mxBean = CacherInitUtil.getBeanInstance(RecordMXBeanImpl.class);
            mBeanServer.registerMBean(mxBean, new ObjectName("com.alibaba.cacher:name=HitRate"));
        }
    }

    @Around("@annotation(com.alibaba.cacher.Cached)")
    public Object readCache(ProceedingJoinPoint pjp) throws Throwable {
        Method method = CacherUtils.getMethod(pjp);
        Cached cached = method.getAnnotation(Cached.class);

        Object result;
        if (CacherSwitcher.isSwitchOn(open, cached, method, pjp.getArgs())) {
            long start = System.currentTimeMillis();

            Pair<CacheKeyHolder, MethodInfoHolder> pair = MethodInfoUtil.getMethodInfo(method);
            CacheKeyHolder cacheKeyHolder = pair.getLeft();
            MethodInfoHolder methodInfoHolder = pair.getRight();

            // multi
            if (cacheKeyHolder.isMulti()) {
                result = multiCacheReader.read(cacheKeyHolder, cached, pjp, methodInfoHolder);
            } else {
                result = singleCacheReader.read(cacheKeyHolder, cached, pjp, methodInfoHolder);
            }
            LOGGER.info("cacher [{}] total cost [{}] ms", cached.cache(), System.currentTimeMillis() - start);
        } else {
            result = pjp.proceed();
        }

        return result;
    }

    @After("@annotation(com.alibaba.cacher.Invalidate)")
    public void removeCache(JoinPoint pjp) throws Throwable {
        Method method = CacherUtils.getMethod(pjp);
        Invalidate invalidate = method.getAnnotation(Invalidate.class);

        if (CacherSwitcher.isSwitchOn(open, invalidate, method, pjp.getArgs())) {
            long start = System.currentTimeMillis();
            Pair<CacheKeyHolder, MethodInfoHolder> pair = MethodInfoUtil.getMethodInfo(method);
            CacheKeyHolder holder = pair.getLeft();

            if (holder.isMulti()) {
                Map[] keyIdPair = KeysCombineUtil.toMultiKey(holder, invalidate.separator(), pjp.getArgs());
                Set<String> keys = ((Map<String, Object>) keyIdPair[1]).keySet();
                cacheManager.remove(invalidate.cache(), keys.toArray(new String[keys.size()]));

                LOGGER.info("multi cache clear, keys: {}", keys);

            } else {
                String key = KeysCombineUtil.toSingleKey(pair.getLeft(), invalidate.separator(), pjp.getArgs());
                cacheManager.remove(invalidate.cache(), key);

                LOGGER.info("single cache clear, key: {}", key);
            }

            LOGGER.info("cacher [{}] clear cost [{}] ms", invalidate.cache(), System.currentTimeMillis() - start);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    private Map<String, ICache> initCaches(Map<String, ICache> caches) {
        Preconditions.checkArgument(!caches.isEmpty(), "at least one ICache implement");

        if (caches.get(DEFAULT) == null) {
            ICache cache = caches.values().iterator().next();
            caches.put(DEFAULT, cache);
        }

        return caches;
    }

    @PreDestroy
    public void tearDown()
            throws MalformedObjectNameException,
            MBeanRegistrationException,
            InstanceNotFoundException {

        if (this.jmxSupport && this.mBeanServer != null) {
            this.mBeanServer.unregisterMBean(new ObjectName("com.vdian.cacher:name=HitRate"));
        }
    }
}
