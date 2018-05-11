package com.github.cachex.manager;

import com.github.cachex.ICache;
import com.github.cachex.di.Singleton;
import com.github.cachex.domain.CacheReadResult;
import com.github.cachex.domain.Pair;
import com.github.cachex.exception.CacheXException;
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
public class CacheXManager {

    private static final Logger ROOT_LOGGER = LoggerFactory.getLogger(CacheXManager.class);

    private static final Logger CACHEX_LOGGER = LoggerFactory.getLogger("com.alibaba.cachex");

    private Pair<String, ICache> defaultCacheImpl;

    private Map<String, Pair<String, ICache>> cachePool = new ConcurrentHashMap<>();

    public void initCachePool(Map<String, ICache> caches) {
        Preconditions.checkArgument(!caches.isEmpty(), "CacheXAspect.caches param can not be empty!!!");

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
            CACHEX_LOGGER.info("cache [{}] read single cost: [{}] ms",
                    cacheImpl.getLeft(),
                    (System.currentTimeMillis() - start));

            return result;
        } catch (Throwable e) {
            ROOT_LOGGER.error("read single cache failed, key: {} ", key, e);
            CACHEX_LOGGER.error("read single cache failed, key: {} ", key, e);
            return null;
        }
    }

    public void writeSingle(String cache, String key, Object value, int expire) {
        if (value != null) {
            try {
                Pair<String, ICache> cacheImpl = getCacheImpl(cache);

                long start = System.currentTimeMillis();
                cacheImpl.getRight().write(key, value, expire);
                CACHEX_LOGGER.info("cache [{}] write single cost: [{}] ms",
                        cacheImpl.getLeft(),
                        (System.currentTimeMillis() - start));

            } catch (Throwable e) {
                ROOT_LOGGER.error("write single cache failed, key: {} ", key, e);
                CACHEX_LOGGER.error("write single cache failed, key: {} ", key, e);
            }
        }
    }

    public CacheReadResult readBatch(String cache, Collection<String> keys) {
        CacheReadResult cacheReadResult;
        if (keys.isEmpty()) {
            cacheReadResult = new CacheReadResult();
        } else {
            try {
                Pair<String, ICache> cacheImpl = getCacheImpl(cache);

                long start = System.currentTimeMillis();
                Map<String, Object> cacheMap = cacheImpl.getRight().read(keys);
                CACHEX_LOGGER.info("cache [{}] read batch cost: [{}] ms",
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

                cacheReadResult = new CacheReadResult(hitValueMap, notHitKeys);
            } catch (Throwable e) {
                ROOT_LOGGER.error("read multi cache failed, keys: {}", keys, e);
                CACHEX_LOGGER.error("read multi cache failed, keys: {}", keys, e);
                cacheReadResult = new CacheReadResult();
            }
        }

        return cacheReadResult;
    }

    public void writeBatch(String cache, Map<String, Object> keyValueMap, int expire) {
        try {
            Pair<String, ICache> cacheImpl = getCacheImpl(cache);

            long start = System.currentTimeMillis();
            cacheImpl.getRight().write(keyValueMap, expire);
            CACHEX_LOGGER.info("cache [{}] write batch cost: [{}] ms",
                    cacheImpl.getLeft(),
                    (System.currentTimeMillis() - start));

        } catch (Exception e) {
            ROOT_LOGGER.error("write map multi cache failed, keys: {}", keyValueMap.keySet(), e);
            CACHEX_LOGGER.error("write map multi cache failed, keys: {}", keyValueMap.keySet(), e);
        }
    }

    public void remove(String cache, String... keys) {
        if (keys != null && keys.length != 0) {
            try {
                Pair<String, ICache> cacheImpl = getCacheImpl(cache);

                long start = System.currentTimeMillis();
                cacheImpl.getRight().remove(keys);
                CACHEX_LOGGER.info("cache [{}] remove cost: [{}] ms",
                        cacheImpl.getLeft(),
                        (System.currentTimeMillis() - start));

            } catch (Throwable e) {
                ROOT_LOGGER.error("remove cache failed, keys: {}: ", keys, e);
                CACHEX_LOGGER.error("remove cache failed, keys: {}: ", keys, e);
            }
        }
    }

    private Pair<String, ICache> getCacheImpl(String cacheName) {
        Pair<String, ICache> cachePair;
        if (Strings.isNullOrEmpty(cacheName)) {
            cachePair = this.defaultCacheImpl;
        } else {
            cachePair = this.cachePool.computeIfAbsent(cacheName, (key) -> {
                throw new CacheXException(String.format("no ICache implementation named [%s], " +
                                "please check the CacheXAspect.caches param config correct",
                        key));
            });
        }

        return cachePair;
    }
}
