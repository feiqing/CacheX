package com.alibaba.cacher.support.shooting;

import com.alibaba.cacher.ShootingMXBean;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.exception.CacherException;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jifang.zjf
 * @since 2017/6/14 下午4:35.
 */
public class ZKShootingMXBeanImpl implements ShootingMXBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKShootingMXBeanImpl.class);

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("cacher:shooting-zk-uploader");
        thread.setDaemon(true);
        return thread;
    });

    private static final String NAME_SPACE = "cacher";

    private volatile boolean isShutdown = false;

    private BlockingQueue<Pair<String, Integer>> hitQueue = new LinkedBlockingQueue<>();

    private BlockingQueue<Pair<String, Integer>> requireQueue = new LinkedBlockingQueue<>();

    private Map<String, DistributedAtomicLong> hitCounterMap = new HashMap<>();

    private Map<String, DistributedAtomicLong> requireCounterMap = new HashMap<>();

    private CuratorFramework client;

    private String hitPrefix;

    private String requirePrefix;

    public ZKShootingMXBeanImpl(String zkServers) {
        this(zkServers, System.getProperty("product.name", "unnamed"));
    }

    public ZKShootingMXBeanImpl(String zkServers, String uniqueProductName) {
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
            LOGGER.info("create path:[{}],[{}] on namespace: [{}] success", hitPrefix, requirePrefix, NAME_SPACE);
        } catch (KeeperException.NodeExistsException ignored) {
            LOGGER.warn("path: [{}], [{}] on namespace: [{}] is exits", hitPrefix, requirePrefix, NAME_SPACE);
        } catch (Exception e) {
            throw new CacherException("create path: " + hitPrefix + ", " + requirePrefix + " on namespace: " + NAME_SPACE + " error", e);
        }

        executor.submit(() -> {
            while (!isShutdown) {
                dumpToZK(hitQueue, hitCounterMap, hitPrefix);
                dumpToZK(requireQueue, requireCounterMap, requirePrefix);
            }
        });
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

    @Override
    public void hitIncr(String pattern, int count) {
        hitQueue.add(Pair.of(pattern, count));
    }

    @Override
    public void requireIncr(String pattern, int count) {
        requireQueue.add(Pair.of(pattern, count));
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

        result.put(summaryName(), ShootingDO.newInstance(totalHit.get(), totalRequire.get()));

        return result;
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

    @Override
    public void reset(String pattern) {
        hitCounterMap.computeIfPresent(pattern, this::doReset);
        requireCounterMap.computeIfPresent(pattern, this::doReset);
    }

    @Override
    public void resetAll() {
        hitCounterMap.forEach(this::doReset);
        requireCounterMap.forEach(this::doReset);
    }

    private DistributedAtomicLong doReset(String pattern, DistributedAtomicLong counter) {
        try {
            counter.forceSet(0L);
        } catch (Exception e) {
            LOGGER.error("reset zk pattern: {} error", pattern, e);
        }

        return null;
    }

    @PreDestroy
    public void tearDown() {
        while (hitQueue.size() > 0 || requireQueue.size() > 0) {
            LOGGER.warn("shooting queue is not empty: [{}]-[{}], waiting...", hitQueue.size(), requireQueue.size());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }

        isShutdown = true;
    }

    private void dumpToZK(BlockingQueue<Pair<String, Integer>> queue, Map<String, DistributedAtomicLong> counterMap, String zkPrefix) {
        long count = 0;
        Pair<String, Integer> head;

        // 将queue中所有的 || 前100条数据聚合到一个暂存Map中
        Map<String, AtomicLong> holdMap = new HashMap<>();
        while ((head = queue.poll()) != null && count <= 100) {
            holdMap
                    .computeIfAbsent(head.getLeft(), (key) -> new AtomicLong(0L))
                    .addAndGet(head.getRight());
            ++count;
        }

        holdMap.forEach((pattern, atomicCount) -> {
            String zkPath = String.format("%s/%s", zkPrefix, pattern);
            DistributedAtomicLong counter = counterMap.computeIfAbsent(pattern, (key) -> new DistributedAtomicLong(client, zkPath, new RetryNTimes(10, 10)));
            try {
                LOGGER.info("zkPath: {} current value : {}", zkPath, counter.add(atomicCount.get()).postValue());
            } catch (Exception e) {
                LOGGER.error("update zkPath: {} count offset:{} error", zkPath, atomicCount);
            }
        });
    }
}
