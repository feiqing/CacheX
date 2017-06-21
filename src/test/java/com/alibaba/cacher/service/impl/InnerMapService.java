package com.alibaba.cacher.service.impl;

import com.alibaba.cacher.CacheKey;
import com.alibaba.cacher.Cached;
import com.alibaba.cacher.domain.User;
import com.alibaba.cacher.enums.Expire;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author jifang.zjf
 * @since 2017/6/21 下午7:25.
 */
@Component
public class InnerMapService {

    @Cached(expire = Expire.TEN_MIN)
    public Map<Integer, User> unmodifiableMap(@CacheKey(prefix = "id:", multi = true) List<Integer> ids) {
        if (ids.size() == 1) {
            int id = ids.get(0);
            return Collections.singletonMap(id, new User(id, "name" + id, new Date(), id, ""));
        } else {
            Map<Integer, User> map = new HashMap<>();
            for (Integer id : ids) {
                map.put(id, new User(id, "name" + id, new Date(), id, ""));
            }

            return Collections.unmodifiableMap(map);
        }
    }

    @Cached(expire = Expire.TEN_MIN)
    public Map<Integer, User> synchronizedMap(@CacheKey(prefix = "id:", multi = true) List<Integer> ids) {
        Map<Integer, User> map = new HashMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, ""));
        }

        return Collections.synchronizedMap(map);
    }

    @Cached(expire = Expire.TEN_MIN)
    public Map<Integer, User> checkedMap(@CacheKey(prefix = "id:", multi = true) List<Integer> ids) {
        TreeMap<Integer, User> map = new TreeMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, ""));
        }

        return Collections.checkedNavigableMap(map, Integer.class, User.class);
    }

    @Cached(expire = Expire.TEN_MIN)
    public Map<Integer, User> immutableMap(@CacheKey(prefix = "id:", multi = true) List<Integer> ids) {
        TreeMap<Integer, User> map = new TreeMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, ""));
        }

        return ImmutableMap.copyOf(map);
    }
}
