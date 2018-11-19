package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.cachex.enums.Expire;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;
import redis.clients.jedis.JedisCluster;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.cachex.support.cache.RedisHelpers.toByteArray;
import static com.github.cachex.support.cache.RedisHelpers.toObjectMap;

/**
 * @author jifang
 * @since 2017/4/6 下午4:24.
 */
public class RedisClusterCache implements ICache {

    private ISerializer serializer;

    private JedisCluster jedisCluster;

    public RedisClusterCache(JedisCluster jedisCluster) {
        this(jedisCluster, new Hessian2Serializer());
    }

    public RedisClusterCache(JedisCluster jedisCluster, ISerializer serializer) {
        this.jedisCluster = jedisCluster;
        this.serializer = serializer;
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
            jedisCluster.setex(key.getBytes(), (int) (expire / 1000), bytes);
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<byte[]> bytesValues = jedisCluster.mget(toByteArray(keys));
        return toObjectMap(keys, bytesValues, this.serializer);
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        if (keyValueMap.isEmpty()) {
            return;
        }

        if (expire == Expire.FOREVER) {
            jedisCluster.mset(toByteArray(keyValueMap, this.serializer));
        } else {
            for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
                write(entry.getKey(), entry.getValue(), expire);
            }
        }
    }

    @Override
    public void remove(String... keys) {
        if (keys.length == 0) {
            return;
        }

        jedisCluster.del(keys);
    }


    @PreDestroy
    public void tearDown() throws IOException {
        if (this.jedisCluster != null) {
            this.jedisCluster.close();
        }
    }
}
