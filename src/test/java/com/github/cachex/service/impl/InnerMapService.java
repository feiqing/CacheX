package com.github.cachex.service.impl;

import com.github.cachex.CacheKey;
import com.github.cachex.Cached;
import com.github.cachex.domain.User;
import com.github.cachex.enums.Expire;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author jifang.zjf
 * @since 2017/6/21 下午7:25.
 */
@Component
public class InnerMapService {

    @Cached(expire = Expire.TEN_MIN)
    public Map<Integer, User> unmodifiableMap(@CacheKey(value = "id:", multi = true) List<Integer> ids) {
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
    public Map<Integer, User> synchronizedMap(@CacheKey(value = "id:", multi = true) List<Integer> ids) {
        Map<Integer, User> map = new HashMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, ""));
        }

        return Collections.synchronizedMap(map);
    }

    @Cached(expire = Expire.TEN_MIN)
    public Map<Integer, User> checkedMap(@CacheKey(value = "id:", multi = true) List<Integer> ids) {
        TreeMap<Integer, User> map = new TreeMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, ""));
        }

        return Collections.checkedNavigableMap(map, Integer.class, User.class);
    }

    @Cached(prefix = "map-", expire = Expire.TEN_MIN)
    public Map<Integer, User> immutableMap(@CacheKey(value = "id:", multi = true) List<Integer> ids) {
        TreeMap<Integer, User> map = new TreeMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, ""));
        }

        return Collections.unmodifiableMap(map);
    }
}
