package com.github.cachex.service.impl;


import com.github.cachex.CacheKey;
import com.github.cachex.Cached;
import com.github.cachex.Invalid;
import com.github.cachex.domain.User;
import com.github.cachex.enums.Expire;
import com.github.cachex.service.UserService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jifang
 * @since 16/3/20 下午5:49.
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * multi
     ***/
    @Override
    @Cached(prefix = "map:", expire = Expire.TEN_MIN)
    public Map<Integer, User> returnMap(@CacheKey String app, @CacheKey("'-' + #arg1[#i]") List<Integer> ids, Object noKey) {
        Map<Integer, User> map = new HashMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, noKey.toString()));
        }
        return map;
    }

    @Override
    @Invalid(prefix = "map:")
    public void invalidMap(@CacheKey String apps, @CacheKey("'-' + #arg1[#i]") List<Integer> ids) {
        System.out.println("method: " + ids);
    }

    @Override
    @Cached(prefix = "[USER]:")
    public List<User> getUsers(@CacheKey(value = "#arg0[#i]", field = "id") List<Integer> ids,
                               @CacheKey("'-' + #arg1") String name, Object non) {
        return ids.stream().map((id) -> new User(id, name)).collect(Collectors.toList());
    }

    @Override
    @Invalid(prefix = "[USER]:")
    public void invalidList(@CacheKey(value = "#arg0[#i].id + '-' + #arg0[#i].name") List<User> users) {
        List<Integer> ids = new ArrayList<>(users.size());
        for (User user : users) {
            ids.add(user.getId());
        }
        System.out.println("method:" + ids);
    }


    /**
     * single
     */

    @Cached(prefix = "list:", expire = Expire.TEN_SEC)
    public User singleKey(@CacheKey int id, String name, Object non) {
        return new User(id, name, new Date(), 1, "山东-德州");
    }

    @Override
    @Invalid(prefix = "list:")
    public void singleRemove(@CacheKey int id, String name, Object non) {
    }

    @Override
    @Invalid(prefix = "list")
    public void updateUser(@CacheKey(value = "id") User user, @CacheKey String name, Object non) {
    }

    @Override
    @Cached
    public boolean spelCompose(@CacheKey(value = "'id:'+#arg0.id+'-name:'+#arg0.name+'-address:'+#arg0.getAddress()+'-time:'+#arg0.getBirthday()") User user) {
        return false;
    }

    /**
     * ops
     */
    @Override
    @Cached(prefix = "total")
    public void noParam() {

    }

    @Override
    @Cached(prefix = "total")
    public void noCacheKey(Object o) {

    }

    @Cached
    @Override
    public void wrongMultiParam(@CacheKey("#arg0[#i]") Object o) {

    }

    @Cached
    @Override
    public Map<Integer, Object> wrongIdentifier(@CacheKey(field = "id") List<Integer> ids) {
        return null;
    }

    @Cached
    @Override
    public List<User> wrongCollectionReturn(@CacheKey(value = "#arg0[#i]") List<Integer> ids) {
        return null;
    }

    @Override
    @Cached
    public List<User> correctIdentifier(@CacheKey(value = "#arg0[#i]", field = "id") List<Integer> ids) {
        List<User> users = new ArrayList<>(2);
        for (int id : ids) {
            users.add(new User(id, "name" + id, new Date(), id, "zj" + id));
        }
        return users;
    }

}
