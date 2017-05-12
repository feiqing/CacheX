package com.alibaba.cacher.support.cache;

import com.alibaba.cacher.ICache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author jifang
 * @since 2017/1/10 上午11:56.
 */
public class NoOpCache implements ICache {

    @Override
    public Object read(String key) {
        return null;
    }

    @Override
    public void write(String key, Object value, long expire) {
        // no op
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        return Collections.emptyMap();
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        // no op
    }

    @Override
    public void remove(String... keys) {
        // no op
    }
}
