package com.alibaba.cacher.service.impl;


import com.alibaba.cacher.Invalid;
import com.alibaba.cacher.service.UserService;
import com.alibaba.cacher.CacheKey;
import com.alibaba.cacher.Cached;
import com.alibaba.cacher.domain.User;
import com.alibaba.cacher.enums.Expire;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author jifang
 * @since 16/3/20 下午5:49.
 */
@Component
public class UserServiceImpl implements UserService {

    /**
     * multi
     ***/
    @Override
    @Cached(cache = "levelDB", prefix = "pix", expire = Expire.FIVE_MIN)
    public Map<Integer, User> returnMap(@CacheKey(prefix = "app:") String app, @CacheKey(prefix = "id:", multi = true) List<Integer> ids, Object noKey) {
        System.out.println("method: " + ids);
        Map<Integer, User> map = new TreeMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, noKey.toString()));
        }
        return map;
    }

    @Override
    @Invalid(cache = "levelDB", prefix = "pix")
    public void multiInvalid(@CacheKey(prefix = "app:") String apps, @CacheKey(prefix = "id:", multi = true) List<Integer> ids) {
        System.out.println("method: " + ids);
    }

    @Override
    @Cached(cache = "redis", prefix = "prefix")
    public List<User> returnList(@CacheKey(prefix = "ids:", multi = true, id = "id") List<Integer> ids, String name, Object non) {
        System.out.println("method:" + ids);
        List<User> list = new LinkedList<>();
        for (int id : ids) {
            User user = new User(id, name, new Date(), id, non.toString());
            list.add(user);
        }

        return list;
    }

    @Override
    @Invalid(cache = "redis", prefix = "prefix")
    public void batchUpdateList(@CacheKey(prefix = "ids:", multi = true, spel = "id") List<User> users) {
        List<Integer> ids = new ArrayList<>(users.size());
        for (User user : users) {
            ids.add(user.getId());
        }
        System.out.println("method:" + ids);
    }

    @Cached(cache = "levelDB", prefix = "prefix")
    public User singleKey(@CacheKey(prefix = "id:") int id, String name, Object non) {
        return new User(id, name, new Date(), 1, "山东-德州");
    }

    @Override
    @Invalid(cache = "redis")
    public void singleRemove(@CacheKey(prefix = "id:") int id, String name, Object non) {
    }

    @Override
    @Invalid(cache = "redis")
    public void updateUser(@CacheKey(prefix = "id:", spel = "id") User user, @CacheKey(prefix = "second:") String name, Object non) {
    }

    @Override
    @Cached(cache = "levelDB")
    public boolean spelCompose(@CacheKey(spel="'id:'+id+'-name:'+name+'-address:'+getAddress()+'-time:'+getBirthday()") User user) {
        return false;
    }

    /**
     * ops
     */
    @Cached(prefix = "total")
    @Override
    public void noParam() {

    }

    @Cached(prefix = "total")
    @Override
    public void noCacheKey(Object o) {

    }

    @Cached
    @Override
    public void wrongMultiParam(@CacheKey(multi = true) Object o) {

    }

    @Cached
    @Override
    public Map<Integer, Object> wrongIdentifier(@CacheKey(multi = true, id = "id") List<Integer> ids) {
        return null;
    }

    @Cached
    @Override
    public List<User> wrongCollectionReturn(@CacheKey(multi = true) List<Integer> ids) {
        return null;
    }

    @Override
    @Cached
    public List<User> correctIdentifier(@CacheKey(multi = true, id = "id", prefix = "id:") List<Integer> ids) {
        List<User> users = new ArrayList<>(2);
        for (int id : ids) {
            users.add(new User(id, "name" + id, new Date(), id, "zj" + id));
        }
        return users;
    }
}
