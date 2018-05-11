package com.github.cachex.core;

import com.github.cachex.Cached;
import com.github.cachex.CachedGet;
import com.github.cachex.ICache;
import com.github.cachex.Invalid;
import com.github.cachex.constant.Constant;
import com.github.cachex.di.CacheXDIContainer;
import com.github.cachex.di.Inject;
import com.github.cachex.di.Singleton;
import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.domain.CacheMethodHolder;
import com.github.cachex.domain.Pair;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheXManager;
import com.github.cachex.reader.AbstractCacheReader;
import com.github.cachex.supplier.CacheXInfoSupplier;
import com.github.cachex.utils.KeyGenerators;
import com.github.cachex.utils.SwitcherUtils;
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
public class CacheXCore {

    protected static final Logger LOGGER = LoggerFactory.getLogger("com.alibaba.cachex");

    @Inject
    private AbstractCacheReader singleCacheReader;

    @Inject
    private AbstractCacheReader multiCacheReader;

    @Inject
    private CacheXManager cacheXManager;

    private MBeanServer mBeanServer;

    private boolean isInited = false;

    public boolean isInited() {
        return isInited;
    }

    /**
     * faced 在CacheX启动之前一定要调用init()方法
     *
     * @param caches
     * @param config
     * @throws MalformedObjectNameException
     * @throws NotCompliantMBeanException
     * @throws InstanceAlreadyExistsException
     * @throws MBeanRegistrationException
     * @throws IOException
     */
    public void init(Map<String, ICache> caches, Config config) throws
            MalformedObjectNameException,
            NotCompliantMBeanException,
            InstanceAlreadyExistsException,
            MBeanRegistrationException,
            IOException {

        CacheXDIContainer.registerBeanInstance(config);
        if (config.getShootingMXBean() != null) {
            CacheXDIContainer.registerBeanInstance(config.getShootingMXBean());
            this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.mBeanServer.registerMBean(config.getShootingMXBean(), new ObjectName("com.alibaba.cachex:name=shooting"));
        }

        CacheXDIContainer.init(this,
                Constant.PACKAGE_MANAGER,
                Constant.PACKAGE_READER
        );
        this.cacheXManager.initCachePool(caches);

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

        Pair<CacheKeyHolder, CacheMethodHolder> pair = CacheXInfoSupplier.getMethodInfo(method);
        CacheKeyHolder cacheKeyHolder = pair.getLeft();
        CacheMethodHolder cacheMethodHolder = pair.getRight();

        Object result;
        if (cacheKeyHolder.isMulti()) {
            result = multiCacheReader.read(cacheKeyHolder, cacheMethodHolder, invoker, needWrite);
        } else {
            result = singleCacheReader.read(cacheKeyHolder, cacheMethodHolder, invoker, needWrite);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cachex read total cost [{}] ms", (System.currentTimeMillis() - start));
        }

        return result;
    }

    public void remove(boolean open, Invalid invalid, Method method, Object[] args) {
        if (SwitcherUtils.isSwitchOn(open, invalid, method, args)) {

            long start = 0;
            if (LOGGER.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }

            CacheKeyHolder cacheKeyHolder = CacheXInfoSupplier.getMethodInfo(method).getLeft();
            if (cacheKeyHolder.isMulti()) {
                Map[] keyIdPair = KeyGenerators.generateMultiKey(cacheKeyHolder, args);
                Set<String> keys = ((Map<String, Object>) keyIdPair[1]).keySet();
                cacheXManager.remove(invalid.cache(), keys.toArray(new String[keys.size()]));

                LOGGER.info("multi cache clear, keys: {}", keys);
            } else {
                String key = KeyGenerators.generateSingleKey(cacheKeyHolder, args);
                cacheXManager.remove(invalid.cache(), key);

                LOGGER.info("single cache clear, key: {}", key);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("cachex clear total cost [{}] ms", (System.currentTimeMillis() - start));
            }
        }
    }

    /**
     * 各faced 在CacheX关闭前调用tearDown()方法
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
            this.mBeanServer.unregisterMBean(new ObjectName("com.alibaba.cachex:name=shooting"));
        }
    }
}
