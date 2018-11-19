package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.cachex.enums.Expire;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.cachex.support.cache.RedisHelpers.toByteArray;
import static com.github.cachex.support.cache.RedisHelpers.toObjectMap;

/**
 * @author jifang
 * @since 2016/12/12 下午3:06.
 */
public class RedisPoolCache implements ICache {

    private ISerializer serializer;

    private JedisPool jedisPool;

    public RedisPoolCache(JedisPool jedisPool) {
        this(jedisPool, new Hessian2Serializer());
    }

    public RedisPoolCache(JedisPool jedisPool, ISerializer serializer) {
        this.jedisPool = jedisPool;
        this.serializer = serializer;
    }

    @Override
    public Object read(String key) {
        try (Jedis client = jedisPool.getResource()) {
            byte[] bytes = client.get(key.getBytes());
            return serializer.deserialize(bytes);
        }
    }

    @Override
    public void write(String key, Object value, long expire) {
        try (Jedis client = jedisPool.getResource()) {
            byte[] bytesValue = serializer.serialize(value);
            if (expire == Expire.FOREVER) {
                client.set(key.getBytes(), bytesValue);
            } else {
                client.psetex(key.getBytes(), expire, bytesValue);
            }
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        try (Jedis client = jedisPool.getResource()) {
            List<byte[]> bytesValues = client.mget(toByteArray(keys));
            return toObjectMap(keys, bytesValues, this.serializer);
        }
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        try (Jedis client = jedisPool.getResource()) {
            byte[][] kvs = toByteArray(keyValueMap, serializer);
            if (expire == Expire.FOREVER) {
                client.mset(kvs);
            } else {
                Pipeline pipeline = client.pipelined();
                for (int i = 0; i < kvs.length; i += 2) {
                    pipeline.psetex(kvs[i], expire, kvs[i + 1]);
                }
                pipeline.sync();
            }
        }
    }

    @Override
    public void remove(String... keys) {
        try (Jedis client = jedisPool.getResource()) {
            client.del(keys);
        }
    }

    @PreDestroy
    public void tearDown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.destroy();
        }
    }
}