package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.cachex.enums.Expire;
import com.github.cachex.utils.SerializeUtils;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hession2Serializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/12/12 下午3:06.
 */
public class RedisCache implements ICache {

    private ISerializer serializer;

    private JedisPool pool;

    public RedisCache(String host, int port) {
        this(host, port, 8, 10);
    }

    public RedisCache(String host, int port, int maxTotal, int waitMillis) {
        this(host, port, maxTotal, waitMillis, new Hession2Serializer());
    }

    public RedisCache(String host, int port, int maxTotal, int waitMillis, ISerializer serializer) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxWaitMillis(waitMillis);

        pool = new JedisPool(config, host, port);
        this.serializer = serializer;
    }

    @Override
    public Object read(String key) {
        try (Jedis client = pool.getResource()) {
            byte[] bytes = client.get(key.getBytes());
            return serializer.deserialize(bytes);
        }
    }

    @Override
    public void write(String key, Object value, long expire) {
        try (Jedis client = pool.getResource()) {
            byte[] bytesValue = serializer.serialize(value);
            if (expire == Expire.FOREVER) {
                client.set(key.getBytes(), bytesValue);
            } else {
                client.setex(key.getBytes(), (int) expire, bytesValue);
            }
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        try (Jedis client = pool.getResource()) {
            List<byte[]> bytesValues = client.mget(SerializeUtils.toByteArray(keys));
            return SerializeUtils.toObjectMap(keys, bytesValues, this.serializer);
        }
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        try (Jedis client = pool.getResource()) {
            byte[][] kvs = SerializeUtils.toByteArray(keyValueMap, serializer);
            if (expire == Expire.FOREVER) {
                client.mset(kvs);
            } else {
                Pipeline pipeline = client.pipelined();
                for (int i = 0; i < kvs.length; i += 2) {
                    pipeline.setex(kvs[i], (int) expire, kvs[i + 1]);
                }
                pipeline.sync();
            }
        }
    }

    @Override
    public void remove(String... keys) {
        try (Jedis client = pool.getResource()) {
            client.del(keys);
        }
    }

    @PreDestroy
    public void tearDown() {
        if (pool != null && !pool.isClosed()) {
            pool.destroy();
        }
    }
}