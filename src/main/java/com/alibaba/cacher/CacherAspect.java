package com.alibaba.cacher;

import com.alibaba.cacher.config.Inject;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.constant.Constant;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.CacheMethodHolder;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.invoker.JoinPointInvokerAdapter;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.reader.AbstractCacheReader;
import com.alibaba.cacher.utils.*;
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
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

    @Inject
    private AbstractCacheReader singleCacheReader;

    @Inject
    private AbstractCacheReader multiCacheReader;

    @Inject
    private CacheManager cacheManager;

    private MBeanServer mBeanServer;

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

        if (this.shootingMXBean != null) {
            CacherIOCContainer.registerBeanInstance(this.shootingMXBean);
            this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.mBeanServer.registerMBean(this.shootingMXBean,
                    new ObjectName("com.alibaba.cacher:name=shooting"));
        }

        CacherIOCContainer.init(this,
                Constant.PACKAGE_MANAGER,
                Constant.PACKAGE_READER
        );
        this.cacheManager.initICachePool(this.caches);
    }

    @Around("@annotation(com.alibaba.cacher.Cached)")
    public Object readWrite(ProceedingJoinPoint pjp) throws Throwable {
        Method method = CacherUtils.getMethod(pjp);
        Cached cached = method.getAnnotation(Cached.class);

        Object result;
        if (CacherSwitcher.isSwitchOn(open, cached, method, pjp.getArgs())) {
            long start = 0;
            if (LOGGER.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }

            Pair<CacheKeyHolder, CacheMethodHolder> pair = MethodInfoUtil.getMethodInfo(method);
            CacheKeyHolder cacheKeyHolder = pair.getLeft();
            CacheMethodHolder cacheMethodHolder = pair.getRight();

            // multi
            if (cacheKeyHolder.isMulti()) {
                result = multiCacheReader.read(cacheKeyHolder, cacheMethodHolder, new JoinPointInvokerAdapter(pjp), true);
            } else {
                result = singleCacheReader.read(cacheKeyHolder, cacheMethodHolder, new JoinPointInvokerAdapter(pjp), true);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("cacher read total cost [{}] ms", (System.currentTimeMillis() - start));
            }
        } else {
            result = pjp.proceed();
        }

        return result;
    }

    @After("@annotation(com.alibaba.cacher.Invalid)")
    public void remove(JoinPoint pjp) throws Throwable {
        Method method = CacherUtils.getMethod(pjp);
        Invalid invalid = method.getAnnotation(Invalid.class);

        if (CacherSwitcher.isSwitchOn(open, invalid, method, pjp.getArgs())) {

            long start = 0;
            if (LOGGER.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }

            Pair<CacheKeyHolder, CacheMethodHolder> pair = MethodInfoUtil.getMethodInfo(method);
            CacheKeyHolder cacheKeyHolder = pair.getLeft();

            if (cacheKeyHolder.isMulti()) {
                Map[] keyIdPair = KeysCombineUtil.toMultiKey(cacheKeyHolder, pjp.getArgs());
                Set<String> keys = ((Map<String, Object>) keyIdPair[1]).keySet();
                cacheManager.remove(invalid.cache(), keys.toArray(new String[keys.size()]));

                LOGGER.info("multi cache clear, keys: {}", keys);

            } else {
                String key = KeysCombineUtil.toSingleKey(cacheKeyHolder, pjp.getArgs());
                cacheManager.remove(invalid.cache(), key);

                LOGGER.info("single cache clear, key: {}", key);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("cacher clear total cost [{}] ms", (System.currentTimeMillis() - start));
            }
        }
    }

    @PreDestroy
    public void tearDown()
            throws MalformedObjectNameException,
            MBeanRegistrationException,
            InstanceNotFoundException {

        if (this.mBeanServer != null
                && this.shootingMXBean != null) {
            this.mBeanServer.unregisterMBean(new ObjectName("com.alibaba.cacher:name=hit"));
        }
    }
}
