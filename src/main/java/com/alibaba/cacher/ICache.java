package com.alibaba.cacher;

import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/2 下午4:58.
 */
public interface ICache {

    Object read(String key);

    void write(String key, Object value, long expire);

    Map<String, Object> read(Collection<String> keys);

    void write(Map<String, Object> keyValueMap, long expire);

    void remove(String... keys);
}