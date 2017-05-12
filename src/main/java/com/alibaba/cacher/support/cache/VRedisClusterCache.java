package com.alibaba.cacher.support.cache;

/*
import com.google.common.collect.Lists;
import ICache;
import IObjectSerializer;
import Hessian2Serializer;
import CacherUtils;
import com.vdian.redis.RedisClusterClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.annotation.PreDestroy;
import java.util.*;

import static Expire.FOREVER;
*/
/**
 * @author jifang
 * @since 2016/12/26 下午3:53.
 */
/*
public class VRedisClusterCache implements ICache {

    private static final int REDIS_CLUSTER_LIMIT = 100;

    private IObjectSerializer serializer;

    private RedisClusterClient clusterClient;

    public VRedisClusterCache(String namespace) {
        this(namespace, 8, 10);
    }

    public VRedisClusterCache(String namespace, int maxTotal, int waitMillis) {
        this(namespace, maxTotal, waitMillis, new Hessian2Serializer());
    }

    public VRedisClusterCache(String namespace, int maxTotal, int waitMillis, IObjectSerializer serializer) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxWaitMillis(waitMillis);

        clusterClient = new RedisClusterClient(namespace, config);
        this.serializer = serializer;
    }

    @Override
    public Object read(String key) {
        return serializer.deserialize(clusterClient.get(key.getBytes()));
    }

    @Override
    public void write(String key, Object value, long expire) {
        byte[] bytes = serializer.serialize(value);
        if (expire == FOREVER) {
            clusterClient.set(key.getBytes(), bytes);
        } else {
            clusterClient.setex(key.getBytes(), (int) expire, bytes);
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Map<String, Object> valueMap;
        // redis cluster read limit 100
        if (keys.size() < REDIS_CLUSTER_LIMIT) {
            valueMap = multiRead(keys);
        } else {
            valueMap = new HashMap<>(keys.size());
            List<List<String>> subKeysList = Lists.partition(new ArrayList<>(keys), REDIS_CLUSTER_LIMIT - 1);
            for (List<String> subKeys : subKeysList) {
                valueMap.putAll(multiRead(subKeys));
            }
        }

        return valueMap;
    }

    private Map<String, Object> multiRead(Collection<String> keys) {
        byte[][] keysArr = CacherUtils.toByteArray(keys);
        Map<byte[], byte[]> bytesMap = clusterClient.mget(keysArr);

        Map<String, Object> valueMap;
        if (bytesMap != null && !bytesMap.isEmpty()) {
            valueMap = new HashMap<>(bytesMap.size());
            for (Map.Entry<byte[], byte[]> entry : bytesMap.entrySet()) {
                String key = new String(entry.getKey());
                Object value = serializer.deserialize(entry.getValue());

                valueMap.put(key, value);
            }
        } else {
            valueMap = Collections.emptyMap();
        }

        return valueMap;
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        if (expire == FOREVER && keyValueMap.size() >= REDIS_CLUSTER_LIMIT) {
            List<Map<byte[], byte[]>> maps = CacherUtils.toByteMap(keyValueMap, REDIS_CLUSTER_LIMIT - 1, serializer);
            for (Map<byte[], byte[]> map : maps) {
                clusterClient.msetnxBytes(map);
            }
        } else {
            Map<byte[], byte[]> bytesMap = CacherUtils.toByteMap(keyValueMap, serializer);

            if (expire == FOREVER) {
                clusterClient.msetBytes(bytesMap);
            } else {
                for (Map.Entry<byte[], byte[]> entry : bytesMap.entrySet()) {
                    byte[] key = entry.getKey();
                    byte[] value = entry.getValue();

                    clusterClient.setex(key, (int) expire, value);
                }
            }
        }
    }

    @Override
    public void remove(String... keys) {
        clusterClient.del(keys);
    }

    @PreDestroy
    public void tearDown() {
        if (clusterClient != null) {
            clusterClient.close();
        }
    }
}
*/