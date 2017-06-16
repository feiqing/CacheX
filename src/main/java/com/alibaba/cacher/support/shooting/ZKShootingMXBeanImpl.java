package com.alibaba.cacher.support.shooting;

import com.alibaba.cacher.exception.CacherException;
import com.alibaba.cacher.ShootingMXBean;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author jifang.zjf
 * @since 2017/6/14 下午4:35.
 */
public class ZKShootingMXBeanImpl implements ShootingMXBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKShootingMXBeanImpl.class);

    private static final String NAME_SPACE = "cacher";

    private static final long _5S = 5 * 1000;

    private ConcurrentMap<String, AtomicLong> hitMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, AtomicLong> requireMap = new ConcurrentHashMap<>();

    private Map<String, DistributedAtomicLong> hitCounterMap = new HashMap<>();

    private Map<String, DistributedAtomicLong> requireCounterMap = new HashMap<>();

    private CuratorFramework client;

    private String hitPrefix;

    private String requirePrefix;

    public ZKShootingMXBeanImpl(String zkServers, String productName) {
        this(zkServers, productName, _5S);
    }

    public ZKShootingMXBeanImpl(String zkServers, String uniqueProductName, long uploadingMs) {
        this.client = CuratorFrameworkFactory.builder()
                .connectString(zkServers)
                .retryPolicy(new RetryNTimes(3, 0))
                .namespace(NAME_SPACE)
                .build();
        client.start();

        // create prefix path
        uniqueProductName = processProductName(uniqueProductName);
        this.hitPrefix = String.format("%s%s", uniqueProductName, "hit");
        this.requirePrefix = String.format("%s%s", uniqueProductName, "require");
        try {
            client.create().creatingParentsIfNeeded().forPath(hitPrefix);
            client.create().creatingParentContainersIfNeeded().forPath(requirePrefix);
            LOGGER.info("create path:[{}],[{}] on namespace: [{}]", hitPrefix, requirePrefix, NAME_SPACE);
        } catch (KeeperException.NodeExistsException ignored) {
            LOGGER.warn("path: [{}], [{}] on namespace: [{}] is exits", hitPrefix, requirePrefix, NAME_SPACE);

        } catch (Exception e) {
            throw new CacherException("create path: " + hitPrefix + ", " + requirePrefix + " on namespace: " + NAME_SPACE + " error", e);
        }

        // register schedule executor
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("cacher:shooting-data-uploader-thread");
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(this::execute, uploadingMs, uploadingMs, TimeUnit.MILLISECONDS);
    }

    private void execute() {
        Map<String, AtomicLong> hitMapTS = new HashMap<>(hitMap);
        hitMap.clear();
        Map<String, AtomicLong> requireMapTS = new HashMap<>(requireMap);
        requireMap.clear();

        hitMapTS.forEach((pattern, count) -> {
            DistributedAtomicLong counter = hitCounterMap.computeIfAbsent(pattern, new AtomicLongInitFunction(hitPrefix));
            try {
                LOGGER.info("hit pattern: {} current value : {}", pattern, counter.add(count.get()).postValue());
            } catch (Exception e) {
                LOGGER.error("update hit pattern: {} count offset:{} error", pattern, count);
            }
        });

        requireMapTS.forEach((pattern, count) -> {
            DistributedAtomicLong counter = requireCounterMap.computeIfAbsent(pattern, new AtomicLongInitFunction(requirePrefix));
            try {
                LOGGER.info("require pattern: {} current value : {}", pattern, counter.add(count.get()).postValue());
            } catch (Exception e) {
                LOGGER.error("update require pattern: {} count offset:{} error", pattern, count);
            }
        });
    }

    @Override
    public void hitIncr(String pattern, int count) {
        hitMap.computeIfAbsent(pattern,
                (k) -> new AtomicLong(0L))
                .addAndGet(count);
    }

    @Override
    public void requireIncr(String pattern, int count) {
        requireMap.computeIfAbsent(pattern,
                (k) -> new AtomicLong(0L))
                .addAndGet(count);
    }

    @Override
    public Map<String, ShootingDO> getShooting() {
        Map<String, ShootingDO> result = new LinkedHashMap<>();

        AtomicLong totalHit = new AtomicLong(0L);
        AtomicLong totalRequire = new AtomicLong(0L);
        this.requireCounterMap.forEach((key, requireCounter) -> {
            try {
                long require = getValue(requireCounter.get());
                long hit = getValue(hitCounterMap.get(key));

                totalRequire.addAndGet(require);
                totalHit.addAndGet(hit);

                result.put(key, ShootingDO.newInstance(hit, require));
            } catch (Exception e) {
                LOGGER.error("current zk counter value get error, pattern: {}", key, e);
            }
        });

        result.put(getSummaryName(), ShootingDO.newInstance(totalHit.get(), totalRequire.get()));

        return result;
    }

    @Override
    public void reset(String pattern) {
        hitCounterMap.computeIfPresent(pattern, resetFunction);
        requireCounterMap.computeIfPresent(pattern, resetFunction);
    }

    @Override
    public void resetAll() {
        hitCounterMap.forEach(this::doReset);
        requireCounterMap.forEach(this::doReset);
    }

    private String processProductName(String productName) {
        if (!productName.startsWith("/")) {
            productName = "/" + productName;
        }

        if (!productName.endsWith("/")) {
            productName = productName + "/";
        }

        return productName;
    }

    private long getValue(Object value) throws Exception {
        long result = 0L;
        if (value != null) {
            if (value instanceof DistributedAtomicLong) {
                result = getValue(((DistributedAtomicLong) value).get());
            } else if (value instanceof AtomicValue) {
                result = (long) ((AtomicValue) value).postValue();
            } else {
                result = ((AtomicLong) value).get();
            }
        }

        return result;
    }

    private BiFunction<String, DistributedAtomicLong, DistributedAtomicLong> resetFunction = (pattern, counter) -> {
        doReset(pattern, counter);
        return null;
    };

    private void doReset(String pattern, DistributedAtomicLong counter) {
        try {
            counter.forceSet(0L);
        } catch (Exception e) {
            LOGGER.error("reset zk pattern: {} counter value error", pattern, e);
        }
    }

    private class AtomicLongInitFunction implements Function<String, DistributedAtomicLong> {

        private String pathDirPrefix;

        public AtomicLongInitFunction(String pathDirPrefix) {
            this.pathDirPrefix = pathDirPrefix;
        }

        @Override
        public DistributedAtomicLong apply(String pattern) {
            String counterPath = String.format("%s/%s", pathDirPrefix, pattern);
            return new DistributedAtomicLong(client, counterPath, new RetryNTimes(10, 10));
        }
    }

    @PreDestroy
    public void tearDown() {
        while (hitMap.size() > 0 || this.requireMap.size() > 0) {
            LOGGER.warn("shooting temporary store map is not empty: [{}]-[{}], waiting...", hitMap.size(), requireMap.size());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
