package com.github.cachex.service.impl;


import com.github.cachex.CacheKey;
import com.github.cachex.Cached;
import com.github.cachex.Invalid;
import com.github.cachex.domain.User;
import com.github.cachex.enums.Expire;
import com.github.cachex.service.UserService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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
    @Cached(prefix = "map", expire = Expire.TEN_MIN)
    public Map<Integer, User> returnMap(@CacheKey(prefix = "app:") String app, @CacheKey(prefix = "id:", multi = true) List<Integer> ids, Object noKey) {
        Map<Integer, User> map = new HashMap<>();
        for (Integer id : ids) {
            map.put(id, new User(id, "name" + id, new Date(), id, noKey.toString()));
        }
        return Collections.synchronizedMap(map);
    }

    @Override
    @Invalid(prefix = "map")
    public void multiInvalid(@CacheKey(prefix = "app:") String apps, @CacheKey(prefix = "id:", multi = true) List<Integer> ids) {
        System.out.println("method: " + ids);
    }

    @Override
    @Cached(prefix = "list-")
    public List<User> returnList(@CacheKey(prefix = "ids:", multi = true, id = "id") List<Integer> ids, @CacheKey(prefix = "-name:") String name, Object non) {
        User[] users = ids.stream().map((id) -> new User(id, name + id)).toArray(User[]::new);

        return Arrays.asList(users);
    }

    @Override
    @Invalid(prefix = "list")
    public void batchUpdateList(@CacheKey(prefix = "id:", multi = true, spel = "id") List<User> users) {
        List<Integer> ids = new ArrayList<>(users.size());
        for (User user : users) {
            ids.add(user.getId());
        }
        System.out.println("method:" + ids);
    }

    /**
     * single
     */

    @Cached(prefix = "list", expire = Expire.TEN_SEC)
    public User singleKey(@CacheKey(prefix = "id:") int id, String name, Object non) {
        return new User(id, name, new Date(), 1, "山东-德州");
    }

    @Override
    @Invalid(prefix = "list")
    public void singleRemove(@CacheKey(prefix = "id:") int id, String name, Object non) {
    }

    @Override
    @Invalid(prefix = "list")
    public void updateUser(@CacheKey(prefix = "id:", spel = "id") User user, @CacheKey(prefix = "second:") String name, Object non) {
    }

    @Override
    @Cached
    public boolean spelCompose(@CacheKey(spel = "'id:'+id+'-name:'+name+'-address:'+getAddress()+'-time:'+getBirthday()") User user) {
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

    @Cached(expire = Expire.TEN_MIN)
    public User getFromDBOrHttp(@CacheKey(prefix = "id:") int id,
                                @CacheKey(prefix = "ad:") String address) {
        // 2. get from db/hsf/dubbo/http
        return new User(id, address);
    }

    @Cached(expire = Expire.FOREVER)
    public List<User> getFromDBOrHttp(@CacheKey(prefix = "id:", multi = true, id = "id") List<Integer> ids,
                                      @CacheKey(prefix = "ad:") String address) {

        return ids.stream().map((id) -> {

            // 4. get from db/hsf/dubbo/http
            return new User(id, address);
        }).collect(Collectors.toList());
    }

    @Invalid
    public void updateUserInfo(@CacheKey(prefix = "id:") int id,
                               @CacheKey(prefix = "ad:") String address) {
        // update db or invoke hsf/dubbo/http ...
    }

}
