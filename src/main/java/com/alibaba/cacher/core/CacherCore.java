package com.alibaba.cacher.core;

import com.alibaba.cacher.*;
import com.alibaba.cacher.constant.Constant;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.CacheMethodHolder;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.invoker.Invoker;
import com.alibaba.cacher.ioc.CacherIOCContainer;
import com.alibaba.cacher.ioc.Inject;
import com.alibaba.cacher.ioc.Singleton;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.reader.AbstractCacheReader;
import com.alibaba.cacher.supplier.CacherInfoSupplier;
import com.alibaba.cacher.utils.KeyGenerators;
import com.alibaba.cacher.utils.SwitcherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author jifang.zjf
 * @since 2017/6/26 下午1:03.
 */
@Singleton
@SuppressWarnings("unchecked")
public class CacherCore {

    protected static final Logger LOGGER = LoggerFactory.getLogger("com.alibaba.cacher");

    @Inject
    private AbstractCacheReader singleCacheReader;

    @Inject
    private AbstractCacheReader multiCacheReader;

    @Inject
    private CacheManager cacheManager;

    private MBeanServer mBeanServer;

    private boolean isInited = false;

    public boolean isInited() {
        return isInited;
    }

    /**
     * faced 在Cacher启动之前一定要调用init()方法
     *
     * @param caches
     * @param shootingMXBean
     * @throws MalformedObjectNameException
     * @throws NotCompliantMBeanException
     * @throws InstanceAlreadyExistsException
     * @throws MBeanRegistrationException
     * @throws IOException
     */
    public void init(Map<String, ICache> caches, ShootingMXBean shootingMXBean) throws
            MalformedObjectNameException,
            NotCompliantMBeanException,
            InstanceAlreadyExistsException,
            MBeanRegistrationException,
            IOException {

        if (shootingMXBean != null) {
            CacherIOCContainer.registerBeanInstance(shootingMXBean);
            this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.mBeanServer.registerMBean(shootingMXBean, new ObjectName("com.alibaba.cacher:name=shooting"));
        }

        CacherIOCContainer.init(this,
                Constant.PACKAGE_MANAGER,
                Constant.PACKAGE_READER
        );
        this.cacheManager.initCachePool(caches);

        this.isInited = true;
    }

    public Object read(boolean open, CachedGet cachedGet, Method method, Invoker invoker) throws Throwable {
        Object result;
        if (SwitcherUtils.isSwitchOn(open, cachedGet, method, invoker.getArgs())) {
            result = doReadWrite(method, invoker, false);
        } else {
            result = invoker.proceed();
        }

        return result;
    }

    public void write() {
        // TODO on @CachedPut
    }

    public Object readWrite(boolean open, Cached cached, Method method, Invoker invoker) throws Throwable {
        Object result;
        if (SwitcherUtils.isSwitchOn(open, cached, method, invoker.getArgs())) {
            result = doReadWrite(method, invoker, true);
        } else {
            result = invoker.proceed();
        }

        return result;
    }

    private Object doReadWrite(Method method, Invoker invoker, boolean needWrite) throws Throwable {
        long start = 0;
        if (LOGGER.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }

        Pair<CacheKeyHolder, CacheMethodHolder> pair = CacherInfoSupplier.getMethodInfo(method);
        CacheKeyHolder cacheKeyHolder = pair.getLeft();
        CacheMethodHolder cacheMethodHolder = pair.getRight();

        Object result;
        if (cacheKeyHolder.isMulti()) {
            result = multiCacheReader.read(cacheKeyHolder, cacheMethodHolder, invoker, needWrite);
        } else {
            result = singleCacheReader.read(cacheKeyHolder, cacheMethodHolder, invoker, needWrite);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cacher read total cost [{}] ms", (System.currentTimeMillis() - start));
        }

        return result;
    }

    public void remove(boolean open, Invalid invalid, Method method, Object[] args) {
        if (SwitcherUtils.isSwitchOn(open, invalid, method, args)) {

            long start = 0;
            if (LOGGER.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }

            CacheKeyHolder cacheKeyHolder = CacherInfoSupplier.getMethodInfo(method).getLeft();
            if (cacheKeyHolder.isMulti()) {
                Map[] keyIdPair = KeyGenerators.generateMultiKey(cacheKeyHolder, args);
                Set<String> keys = ((Map<String, Object>) keyIdPair[1]).keySet();
                cacheManager.remove(invalid.cache(), keys.toArray(new String[keys.size()]));

                LOGGER.info("multi cache clear, keys: {}", keys);
            } else {
                String key = KeyGenerators.generateSingleKey(cacheKeyHolder, args);
                cacheManager.remove(invalid.cache(), key);

                LOGGER.info("single cache clear, key: {}", key);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("cacher clear total cost [{}] ms", (System.currentTimeMillis() - start));
            }
        }
    }

    /**
     * 各faced 在Cacher关闭前调用tearDown()方法
     *
     * @throws MalformedObjectNameException
     * @throws MBeanRegistrationException
     * @throws InstanceNotFoundException
     */
    public void tearDown()
            throws MalformedObjectNameException,
            MBeanRegistrationException,
            InstanceNotFoundException {

        if (this.mBeanServer != null) {
            this.mBeanServer.unregisterMBean(new ObjectName("com.alibaba.cacher:name=shooting"));
        }
    }
}
