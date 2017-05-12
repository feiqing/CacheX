package com.alibaba.cacher.support.cache;

import com.alibaba.cacher.ICache;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 2017/1/10 上午10:36.
 */
public class MapDBCache implements ICache {

    private HTreeMap<String, Object> mapDBCache;

    /**
     * @param interval     : Specifies   that each entry should be automatically removed from the map once a fixed duration has elapsed after the entry's creation,
     *                     or the most recent replacement of its value.
     * @param maxSize      : maximal number of entries in this map. Less used entries will be expired and removed to make collection smaller
     * @param maxStoreSize : maximal size of store in GB, if store is larger entries will start expiring
     */
    public MapDBCache(int interval, long maxSize, double maxStoreSize) {
        mapDBCache = DBMaker.hashMapSegmentedMemoryDirect()
                .expireAfterWrite(interval)
                .expireMaxSize(maxSize)
                .expireStoreSize(maxStoreSize)
                .make();
    }

    @Override
    public Object read(String key) {
        return mapDBCache.get(key);
    }

    @Override
    public void write(String key, Object value, long expire) {
        mapDBCache.put(key, value);
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Map<String, Object> subCache = new HashMap<>(keys.size());
        for (String key : keys) {
            subCache.put(key, read(key));
        }

        return subCache;
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        mapDBCache.putAll(keyValueMap);
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) {
            mapDBCache.remove(key);
        }
    }

    @PreDestroy
    public void tearDown() {
        if (this.mapDBCache != null) {
            this.mapDBCache.clear();
            this.mapDBCache.close();
        }
    }
}
