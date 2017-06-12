package com.alibaba.cacher.manager;

import com.alibaba.cacher.ICache;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.domain.BatchReadResult;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.exception.CacherException;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 16/7/7.
 */
@Singleton
public class CacheManager {

    private static final Logger ROOT_LOGGER = LoggerFactory.getLogger(CacheManager.class);

    private static final Logger CACHER_LOGGER = LoggerFactory.getLogger("com.alibaba.cacher");

    private Pair<String, ICache> defaultCacheImpl;

    private Map<String, ICache> iCachePool = new ConcurrentHashMap<>();

    public void setICachePool(final Map<String, ICache> caches) {
        initDefaultCache(caches);
        this.iCachePool.putAll(caches);
    }

    public Pair<String, ICache> getDefaultCacheImpl() {
        return this.defaultCacheImpl;
    }

    public Object readSingle(String cache, String key) throws Exception {
        try {
            return getCacheImpl(cache).read(key);
        } catch (Throwable e) {
            ROOT_LOGGER.error("read single cache failed, key: {} ", key, e);
            CACHER_LOGGER.error("read single cache failed, key: {} ", key, e);
            return null;
        }
    }

    public void writeSingle(String cache, String key, Object value, int expire) throws Exception {
        if (value != null) {
            try {
                getCacheImpl(cache).write(key, value, expire);
            } catch (Throwable e) {
                ROOT_LOGGER.error("write single cache failed, key: {} ", key, e);
                CACHER_LOGGER.error("write single cache failed, key: {} ", key, e);
            }
        }
    }

    public BatchReadResult readBatch(String cache, Collection<String> keys) throws Exception {
        BatchReadResult batchReadResult;
        if (keys.isEmpty()) {
            batchReadResult = new BatchReadResult();
        } else {
            try {

                Map<String, Object> fromCacheMap = getCacheImpl(cache).read(keys);

                // collect not nit keys, keep order when full hitrate
                Map<String, Object> hitValueMap = new LinkedHashMap<>();
                Set<String> notHitKeys = new LinkedHashSet<>();
                for (String key : keys) {
                    Object value = fromCacheMap.get(key);

                    if (value == null) {
                        notHitKeys.add(key);
                    } else {
                        hitValueMap.put(key, value);
                    }
                }

                batchReadResult = new BatchReadResult(hitValueMap, notHitKeys);
            } catch (Throwable e) {
                ROOT_LOGGER.error("read multi cache failed, keys: {}", keys, e);
                CACHER_LOGGER.error("read multi cache failed, keys: {}", keys, e);
                batchReadResult = new BatchReadResult();
            }
        }

        return batchReadResult;
    }

    public void writeBatch(String cache, Map<String, Object> keyValueMap, int expire) throws Exception {
        try {
            getCacheImpl(cache).write(keyValueMap, expire);
        } catch (Exception e) {
            ROOT_LOGGER.error("write map multi cache failed, keys: {}", keyValueMap.keySet(), e);
            CACHER_LOGGER.error("write map multi cache failed, keys: {}", keyValueMap.keySet(), e);
        }
    }

    public void remove(String cache, String... keys) throws Exception {
        if (keys != null && keys.length != 0) {
            try {
                getCacheImpl(cache).remove(keys);
            } catch (Throwable e) {
                ROOT_LOGGER.error("remove cache failed, keys: {}: ", keys, e);
                CACHER_LOGGER.error("remove cache failed, keys: {}: ", keys, e);
            }
        }
    }

    private void initDefaultCache(Map<String, ICache> caches) {
        Iterator<String> keyIter = caches.keySet().iterator();
        if (keyIter.hasNext()) {
            String cacheName = keyIter.next();
            ICache cacheImpl = caches.get(cacheName);

            this.defaultCacheImpl = Pair.of(cacheName, cacheImpl);
        } else {
            throw new CacherException("CacherAspect.caches param can not be empty!!!");
        }
    }

    private ICache getCacheImpl(String cacheName) {
        ICache cache;
        if (Strings.isNullOrEmpty(cacheName)) {
            cache = this.defaultCacheImpl.getRight();
        } else {
            cache = this.iCachePool.computeIfAbsent(cacheName, (key) -> {
                String msg = String.format("no ICache implementation named [%s], " +
                                "please check the CacherAspect.caches param config correct",
                        key);

                CacherException exception = new CacherException(msg);
                ROOT_LOGGER.error("wrong cache name {}", key, exception);
                CACHER_LOGGER.error("wrong cache name {}", key, exception);

                throw exception;
            });
        }

        return cache;
    }
}
