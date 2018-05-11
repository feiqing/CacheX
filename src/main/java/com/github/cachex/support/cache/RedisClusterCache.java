package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.cachex.enums.Expire;
import com.github.cachex.utils.SerializeUtils;
import com.github.jbox.serializer.ISerializer;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jifang
 * @since 2017/4/6 下午4:24.
 */
public class RedisClusterCache implements ICache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisClusterCache.class);

    private ISerializer serializer;

    private JedisCluster jedisCluster;

    private volatile ExecutorService executor;

    private AtomicInteger count = new AtomicInteger(0);

    /**
     * @param connectString   like 127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
     * @param maxTotal
     * @param waitMillis
     * @param timeout
     * @param maxRedirections
     */
    public RedisClusterCache(String connectString,
                             int maxTotal, int waitMillis, int timeout, int maxRedirections,
                             ISerializer serializer) {
        Set<HostAndPort> nodes = new HashSet<>();
        for (String hostAndPortStr : Splitter.on(",").omitEmptyStrings().trimResults().split(connectString)) {
            String[] hostAndPort = hostAndPortStr.split(":");
            nodes.add(new HostAndPort(hostAndPort[0], Integer.valueOf(hostAndPort[1])));
        }

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxWaitMillis(waitMillis);

        this.jedisCluster = new JedisCluster(nodes, timeout, maxRedirections, config);
        this.serializer = serializer;

        if (Runtime.getRuntime().availableProcessors() >= 4) {
            this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "redis-multi-thread-" + count.getAndIncrement());
                }
            });
        }
    }

    @Override
    public Object read(String key) {
        return serializer.deserialize(jedisCluster.get(key.getBytes()));
    }

    @Override
    public void write(String key, Object value, long expire) {
        byte[] bytes = serializer.serialize(value);
        if (expire == Expire.FOREVER) {
            jedisCluster.set(key.getBytes(), bytes);
        } else {
            jedisCluster.setex(key.getBytes(), (int) expire, bytes);
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        if (keys.size() == 1) {
            return singleKeyMGet(keys);
        }

        if (executor != null) {
            return multiThreadsMGet(keys);
        }

        return singleThreadMGet(keys);
    }

    private Map<String, Object> singleKeyMGet(Collection<String> key) {
        List<byte[]> bytesValues = jedisCluster.mget(SerializeUtils.toByteArray(key));
        return SerializeUtils.toObjectMap(key, bytesValues, this.serializer);
    }

    private Map<String, Object> multiThreadsMGet(Collection<String> keys) {
        List<Future<Object>> futures = new ArrayList<>(keys.size());
        for (final String key : keys) {
            futures.add(
                    executor.submit(new Callable<Object>() {

                        @Override
                        public Object call() throws Exception {
                            return read(key);
                        }
                    })
            );
        }

        Map<String, Object> result = new HashMap<>();
        int index = 0;
        for (String key : keys) {
            try {
                result.put(key, futures.get(index).get());
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("redis get execute error, key:{}", key, e);
            }
        }

        return result;
    }

    private Map<String, Object> singleThreadMGet(Collection<String> keys) {
        Map<String, Object> result = new HashMap<>(keys.size());
        for (String key : keys) {
            result.put(key, read(key));
        }

        return result;
    }

    @Override
    // 单线程set, 多线程set
    public void write(Map<String, Object> keyValueMap, long expire) {
        if (keyValueMap.isEmpty()) {
            return;
        }

        if (keyValueMap.size() == 1) {
            singleKeyValueMSet(keyValueMap, expire);
            return;
        }

        if (executor != null) {
            multiThreadsMSet(keyValueMap, expire);
            return;
        }

        singleThreadMSet(keyValueMap, expire);
    }

    private void singleKeyValueMSet(Map<String, Object> keyValue, long expire) {
        byte[][] bytes = SerializeUtils.toByteArray(keyValue, this.serializer);
        if (expire == Expire.FOREVER) {
            jedisCluster.mset(bytes);
        } else {
            for (Map.Entry<String, Object> entry : keyValue.entrySet()) {
                write(entry.getKey(), entry.getValue(), expire);
            }
        }
    }

    private void multiThreadsMSet(Map<String, Object> keyValueMap, final long expire) {
        final CountDownLatch latch = new CountDownLatch(keyValueMap.size());
        for (final Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    write(entry.getKey(), entry.getValue(), expire);

                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("latch await error", e);
        }
    }

    private void singleThreadMSet(Map<String, Object> keyValueMap, long expire) {
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            write(entry.getKey(), entry.getValue(), expire);
        }
    }

    @Override
    public void remove(String... keys) {
        if (keys.length == 0) {
            return;
        }

        if (keys.length == 1) {
            jedisCluster.del(keys);
            return;
        }

        if (executor != null) {
            multiThreadsDel(keys);
            return;
        }

        singleThreadDel(keys);
    }

    private void multiThreadsDel(String... keys) {
        final CountDownLatch latch = new CountDownLatch(keys.length);
        for (final String key : keys) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    jedisCluster.del(key);
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("latch await error", e);
        }
    }

    private void singleThreadDel(String... keys) {
        for (String key : keys) {
            jedisCluster.del(key);
        }
    }

    @PreDestroy
    public void tearDown() throws IOException {
        if (this.jedisCluster != null) {
            this.jedisCluster.close();
        }
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }
}
