package com.github.cachex.service.impl;

import com.github.cachex.CacheKey;
import com.github.cachex.Cached;
import com.github.cachex.domain.User;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author jifang.zjf
 * @since 2017/7/6 上午11:14.
 */
@Component
public class PreventBreakdownServiceImpl {

    @Cached
    public Map<Integer, User> getMap(@CacheKey("'id:' + #arg0[#i]") List<Integer> ids) {
        Map<Integer, User> map = new HashMap<>(ids.size());
        // 故意不返回第一个
        for (int i = 1; i < ids.size(); ++i) {
            map.put(ids.get(i), new User(ids.get(i), "name" + ids.get(i)));
        }

        return map;
    }

    @Cached
    public List<User> getUsers(@CacheKey(value = "'id:' + #arg0[#i]") Set<Integer> ids) {
        List<User> u = new ArrayList<>();
        for (int i : ids) {
            u.add(new User(i, "name" + i));
        }

        return u;
    }

    @Cached
    public List<User> getUsers2(@CacheKey(value = "'id:' + #arg0.keySet()[#i]", field = "id") Map<Integer, Object> ids) {
        List<User> u = new ArrayList<>();
        for (int i : ids.keySet()) {
            u.add(new User(i, "name" + i));
        }

        return u;
    }
}
