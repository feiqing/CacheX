package com.alibaba.cacher.support.cache;

import com.alibaba.cacher.IObjectSerializer;
import com.alibaba.cacher.exception.CacherException;
import com.alibaba.cacher.support.serialize.Hessian2Serializer;
import com.alibaba.cacher.ICache;
import com.alibaba.cacher.enums.Expire;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author jifang
 * @since 2016/12/12 下午4:05.
 */
public class MemcachedCache implements ICache {

    private static final int _30_DAYS = 60 * 60 * 24 * 30;

    private MemcachedClient client;

    private IObjectSerializer serializer;

    public MemcachedCache(String ipPorts) throws IOException {
        this(ipPorts, new Hessian2Serializer());
    }

    public MemcachedCache(String addressList, IObjectSerializer serializer) throws IOException {
        client = new XMemcachedClientBuilder(addressList).build();
        this.serializer = serializer;
    }

    @Override
    public Object read(String key) {
        try {
            byte[] bytes = client.get(key);
            return serializer.deserialize(bytes);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            throw new CacherException(e);
        }
    }

    @Override
    public void write(String key, Object value, long expire) {
        byte[] byteValue = serializer.serialize(value);
        try {
            if (expire == Expire.FOREVER) {
                client.set(key, _30_DAYS, byteValue);
            } else {
                client.set(key, (int) expire, byteValue);
            }
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            throw new CacherException(e);
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        try {
            Map<String, byte[]> byteMap = client.get(keys);
            Map<String, Object> resultMap = new HashMap<>(byteMap.size());
            for (Map.Entry<String, byte[]> entry : byteMap.entrySet()) {
                String key = entry.getKey();
                Object value = serializer.deserialize(entry.getValue());

                resultMap.put(key, value);
            }

            return resultMap;
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            throw new CacherException(e);
        }
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            this.write(entry.getKey(), entry.getValue(), expire);
        }
    }

    @Override
    public void remove(String... keys) {
        try {
            for (String key : keys) {
                client.delete(key);
            }
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            throw new CacherException(e);
        }
    }

    @PreDestroy
    public void tearDown() {
        if (client != null && !client.isShutdown()) {
            try {
                client.shutdown();
            } catch (IOException e) {
                throw new CacherException(e);
            }
        }
    }
}
