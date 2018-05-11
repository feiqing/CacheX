package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.google.common.collect.Sets;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 2017/1/10 上午11:18.
 */
public class EhCache implements ICache {

    private PersistentCacheManager cacheManager;
    private Cache<String, Serializable> ehcache;

    public EhCache(long heapEntries, long offHeapMBSize, long diskGBSize) {
        this(heapEntries, offHeapMBSize, System.getProperty("user.home") + "/.EhCache", diskGBSize);
    }

    public EhCache(long heapEntries, long offHeapMBSize, String diskPath, long diskGBSize) {

        ResourcePools resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(heapEntries, EntryUnit.ENTRIES)
                .offheap(offHeapMBSize, MemoryUnit.MB)
                .disk(diskGBSize, MemoryUnit.GB)
                .build();

        CacheConfiguration<String, Serializable> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Serializable.class, resourcePools)
                .build();

        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(diskPath))
                .withCache("cachex", configuration)
                .build(true);

        ehcache = cacheManager.getCache("cachex", String.class, Serializable.class);
    }

    @Override
    public Object read(String key) {
        return ehcache.get(key);
    }

    @Override
    public void write(String key, Object value, long expire) {
        ehcache.put(key, (Serializable) value);
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Map<String, Object> map = new HashMap<>(keys.size());
        for (String key : keys) {
            map.put(key, ehcache.get(key));
        }

        return map;
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        Map<String, Serializable> map = new HashMap<>(keyValueMap.size());
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            Object value = entry.getValue();
            map.put(entry.getKey(), (Serializable) value);
        }
        ehcache.putAll(map);
    }

    @Override
    public void remove(String... keys) {
        ehcache.removeAll(Sets.newHashSet(keys));
    }

    @PreDestroy
    public void tearDown() {
        if (this.cacheManager != null) {
            this.cacheManager.close();
        }
    }
}
