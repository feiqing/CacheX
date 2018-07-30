package com.github.cachex.service.impl;

import com.github.cachex.CacheKey;
import com.github.cachex.Cached;
import com.github.cachex.domain.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jifang.zjf
 * @since 2017/7/6 上午11:14.
 */
@Component
public class PreventBreakdownServiceImpl {

    @Cached
    public Map<Integer, User> getMap(@CacheKey(multi = true) List<Integer> ids) {
        Map<Integer, User> map = new HashMap<>(ids.size());
        // 故意不返回第一个
        for (int i = 1; i < ids.size(); ++i) {
            map.put(ids.get(i), new User(ids.get(i), "name" + ids.get(i)));
        }

        return map;
    }
}
