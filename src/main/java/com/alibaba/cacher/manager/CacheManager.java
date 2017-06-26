package com.alibaba.cacher.manager;

import com.alibaba.cacher.ICache;
import com.alibaba.cacher.domain.BatchReadResult;
import com.alibaba.cacher.domain.Pair;
import com.alibaba.cacher.exception.CacherException;
import com.alibaba.cacher.ioc.Singleton;
import com.google.common.base.Preconditions;
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

    private Map<String, Pair<String, ICache>> cachePool = new ConcurrentHashMap<>();

    public void initICachePool(Map<String, ICache> caches) {
        Preconditions.checkArgument(!caches.isEmpty(), "CacherAspect.caches param can not be empty!!!");

        // default cache impl
        Map.Entry<String, ICache> entry = caches.entrySet().iterator().next();
        this.defaultCacheImpl = Pair.of(entry.getKey(), entry.getValue());

        caches.forEach((name, cache) -> this.cachePool.put(name, Pair.of(name, cache)));
    }

    public Object readSingle(String cache, String key) {
        try {
            Pair<String, ICache> cacheImpl = getCacheImpl(cache);

            long start = System.currentTimeMillis();
            Object result = cacheImpl.getRight().read(key);
            CACHER_LOGGER.info("cache [{}] read single cost: [{}] ms",
                    cacheImpl.getLeft(),
                    (System.currentTimeMillis() - start));

            return result;
        } catch (Throwable e) {
            ROOT_LOGGER.error("read single cache failed, key: {} ", key, e);
            CACHER_LOGGER.error("read single cache failed, key: {} ", key, e);
            return null;
        }
    }

    public void writeSingle(String cache, String key, Object value, int expire) {
        if (value != null) {
            try {
                Pair<String, ICache> cacheImpl = getCacheImpl(cache);

                long start = System.currentTimeMillis();
                cacheImpl.getRight().write(key, value, expire);
                CACHER_LOGGER.info("cache [{}] write single cost: [{}] ms",
                        cacheImpl.getLeft(),
                        (System.currentTimeMillis() - start));

            } catch (Throwable e) {
                ROOT_LOGGER.error("write single cache failed, key: {} ", key, e);
                CACHER_LOGGER.error("write single cache failed, key: {} ", key, e);
            }
        }
    }

    public BatchReadResult readBatch(String cache, Collection<String> keys) {
        BatchReadResult batchReadResult;
        if (keys.isEmpty()) {
            batchReadResult = new BatchReadResult();
        } else {
            try {
                Pair<String, ICache> cacheImpl = getCacheImpl(cache);

                long start = System.currentTimeMillis();
                Map<String, Object> cacheMap = cacheImpl.getRight().read(keys);
                CACHER_LOGGER.info("cache [{}] read batch cost: [{}] ms",
                        cacheImpl.getLeft(),
                        (System.currentTimeMillis() - start));

                // collect not nit keys, keep order when full shooting
                Map<String, Object> hitValueMap = new LinkedHashMap<>();
                Set<String> notHitKeys = new LinkedHashSet<>();
                for (String key : keys) {
                    Object value = cacheMap.get(key);

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

    public void writeBatch(String cache, Map<String, Object> keyValueMap, int expire) {
        try {
            Pair<String, ICache> cacheImpl = getCacheImpl(cache);

            long start = System.currentTimeMillis();
            cacheImpl.getRight().write(keyValueMap, expire);
            CACHER_LOGGER.info("cache [{}] write batch cost: [{}] ms",
                    cacheImpl.getLeft(),
                    (System.currentTimeMillis() - start));

        } catch (Exception e) {
            ROOT_LOGGER.error("write map multi cache failed, keys: {}", keyValueMap.keySet(), e);
            CACHER_LOGGER.error("write map multi cache failed, keys: {}", keyValueMap.keySet(), e);
        }
    }

    public void remove(String cache, String... keys) {
        if (keys != null && keys.length != 0) {
            try {
                Pair<String, ICache> cacheImpl = getCacheImpl(cache);

                long start = System.currentTimeMillis();
                cacheImpl.getRight().remove(keys);
                CACHER_LOGGER.info("cache [{}] remove cost: [{}] ms",
                        cacheImpl.getLeft(),
                        (System.currentTimeMillis() - start));

            } catch (Throwable e) {
                ROOT_LOGGER.error("remove cache failed, keys: {}: ", keys, e);
                CACHER_LOGGER.error("remove cache failed, keys: {}: ", keys, e);
            }
        }
    }

    private Pair<String, ICache> getCacheImpl(String cacheName) {
        Pair<String, ICache> cachePair;
        if (Strings.isNullOrEmpty(cacheName)) {
            cachePair = this.defaultCacheImpl;
        } else {
            cachePair = this.cachePool.computeIfAbsent(cacheName, (key) -> {
                throw new CacherException(String.format("no ICache implementation named [%s], " +
                                "please check the CacherAspect.caches param config correct",
                        key));
            });
        }

        return cachePair;
    }
}
