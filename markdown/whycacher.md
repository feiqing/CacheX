## 引入
- 在没有像cacher、spring-cache这类工具的时候, 我们想要在项目中引入缓存提高系统性能/吞吐量往往需要做一下步骤(具体到一个方法实现):
    1. 首先根据参数拼装出一个缓存Key, 查询缓存(可以是Redis、Memcached、EhCache之类的专业缓存服务, 也可以是Guava、HashTable等本地缓存实现);
    2. 如果缓存未命中, 则查询DB, 或HTTP调用, 或Dubbo、HSF、RMI等远程过程调用拿到数据;
    3. 写入缓存然后返回.
这样就可以在下次执行该方法时直接走缓存而不会再执行上面这些耗时的操作.

但上面的这几个步骤说起来容易, 实现起来却很难(模板化、繁琐), 如果每个需要走缓存的方法都这样写一套的话, 不累死也会烦死.

- 根据id和address查询一个用户信息:

```
public User getFromDBOrHttp(int id, String address) {
    // 0. compose cache key
    String key = genCacheKey(id, address);

    // 1. read cache
    User user = (User) cache.read(key);
    if (user == null) {
        // 2. select db or http
        user = new User(id, address);

        // 3. write cache
        cache.write(key, user, 1000);
    }

    return user;
}

private String genCacheKey(int id, String address) {
    return String.format("id:%s-address:%s", id, address);
}
```

如果说上面这个方法实现起来还尚觉简单(或者可以为你的职业生涯增加点代码量点缀), 那下面这个方面就肯定会让你抓狂了:

- 批量获取用户信息

```
public List<User> getFromDBOrHttp(List<Integer> ids, String address) {

    // 0. compose cache key
    List<String> keys = new ArrayList<>(ids.size());
    for (int id : ids) {
        keys.add(genCacheKey(id, address));
    }

    // 2. read cache
    Map<String, Object> fromCache = cache.read(keys);
    List<User> users = new ArrayList<>(ids.size());

    // 3. collect id not in cache
    for (Iterator<Integer> iter = ids.iterator(); iter.hasNext(); ) {
        int id = iter.next();
        String key = genCacheKey(id, address);
        User user = (User) fromCache.get(key);

        // if in cache
        if (user != null) {
            users.add(user);
            iter.remove();
        }
    }

    // 4. select db or http or rmi
    Map<String, Object> needSaveCache = new HashMap<>(ids.size());
    for (int id : ids) {
        User user = new User(id, address);
        String key = genCacheKey(id, address);

        needSaveCache.put(key, user);
        users.add(user);
    }

    // 5. write cache
    if (!needSaveCache.isEmpty()) {
        cache.write(needSaveCache, 1000);
    }

    return users;
}
```

方法大致的思路是首先根据批量id生成一堆key去查询缓存, 如果有部分未命中的, 则查询DB or HTTP ..., 然后再将这些未命中的内容写入缓存.

> 如果说上面的这些傻瓜代码(步骤0、1、2、3、5)你还可以忍受的话(那我也是受不了你了(⊙﹏⊙)b), 那就再来点猛的: 如说公司的缓存策略换了, 大家发现开源界又出现了一个性能更好的缓存服务, 让你把上面的所有代码全部换成新的, 或者你师兄说DB/HTTP性能够用了, 我们不需要缓存了, 你得把上面的这些劳什子全部删掉... 这时候除了一句**WTF**之外你应该啥也说不出来吧. 综上所述, 前面的这种方式不光开发起来慢, 而且维护成本也高. 下面就到了我们的**Cacher**上场的时候了.
