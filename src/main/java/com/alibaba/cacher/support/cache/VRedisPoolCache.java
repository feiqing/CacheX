package com.alibaba.cacher.support.cache;

/*
import ICache;
import Expire;
import Hessian2Serializer;
import IObjectSerializer;
import CacherUtils;
import com.vdian.redis.RedisPoolClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
*/
/**
 * @author jifang
 * @since 2016/11/2 下午5:08.
 */
/*
public class VRedisPoolCache implements ICache {

    private IObjectSerializer serializer;

    private RedisPoolClient pool;

    public VRedisPoolCache(String namespace) {
        this(namespace, 8, 10);
    }

    public VRedisPoolCache(String namespace, int maxTotal, int waitMillis) {
        this(namespace, maxTotal, waitMillis, new Hessian2Serializer());
    }

    public VRedisPoolCache(String namespace, int maxTotal, int waitMillis, IObjectSerializer serializer) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxWaitMillis(waitMillis);

        this.pool = new RedisPoolClient(namespace, config);
        this.serializer = serializer;
    }

    @Override
    public Object read(String key) {
        byte[] bytes = pool.get(key.getBytes());

        return serializer.deserialize(bytes);
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        byte[][] keysArr = CacherUtils.toByteArray(keys);
        List<byte[]> byteValues = pool.mget(keysArr);

        Map<String, Object> valueMap;
        if (byteValues != null && !byteValues.isEmpty()) {
            valueMap = new HashMap<>(byteValues.size());

            int i = 0;
            for (String key : keys) {
                byte[] byteValue = byteValues.get(i++);
                if (byteValue != null && byteValue.length != 0) {
                    Object value = serializer.deserialize(byteValue);

                    valueMap.put(key, value);
                }
            }
        } else {
            valueMap = Collections.emptyMap();
        }

        return valueMap;
    }

    @Override
    public void write(String key, Object value, long expire) {
        byte[] byteValue = serializer.serialize(value);

        if (expire == Expire.FOREVER) {
            pool.set(key.getBytes(), byteValue);
        } else {
            pool.setex(key.getBytes(), (int) expire, byteValue);
        }
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        Map<byte[], byte[]> keyValueBytes = CacherUtils.mapSerialize(keyValueMap, serializer);
        if (expire != Expire.FOREVER) {
            pool.msetbex(keyValueBytes, (int) expire, TimeUnit.SECONDS);
        } else {
            pool.msetb(keyValueBytes);
        }
    }

    @Override
    public void remove(String... keys) {
        pool.del(keys);
    }

    @PreDestroy
    public void tearDown() {
        if (pool != null) {
            pool.close();
        }
    }
}
*/