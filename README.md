# Cacher 缓存注解框架(0.5版本)

标签 ： 开源

---

>  服务于feedcenter的 ***redisCli-annotation***:
1. 每天提供200W+次Dubbo调用, 2.6亿+次缓存查询, 可以做到单次查询(get/mget)耗时 0-2ms (1-200个key).
2. 0.1版本重新设计&开发, 不再与某一具体缓存服务绑定, 提供更灵活的配置、更快的读写效率.

---

## 架构
![](http://7xrgh9.com1.z0.glb.clouddn.com/17-2-20/77360174-file_1487569102103_b75c.png)
> cacher 0.3.X版本.

---
## 版本历史
### 0.0.X
- 即最初的redisCli-annotation, 作为一个杭州Redis Client的封装提供给如下应用使用
    - feedcenter
    - feedcenter-push
    - feedcenter-admin
- 文档
    - [redis-annotation 文档](https://github.com/feiqing/Cacher/wiki/redisCli-annotation-%E6%96%87%E6%A1%A3)
    - [redis-annotation 分享](https://github.com/feiqing/Cacher/wiki/redisCli-annotation-%E5%88%86%E4%BA%AB)

---
### 0.1.X
- 0.1.3
    - 重构为Cacher
        - 开放`ICache`接口以支持更多的缓存服务(如Memcached、Redis、Guava、`ConcurrentHashMap`...).
        - 不再强依赖某一特定缓存产品, 而是作为`ICache`接口的一个具体实现.
    - 添加GuavaCache作为本地Cache默认实现
    - ~~添加VdianRedis作为远程Cache服务实现(开源版本无)~~
- 0.1.4
    - 添加全局缓存开关open参数
- 0.1.5
    - 开放`com.vdian.cacher.serialize.IObjectSerializer`缓存序列化接口(默认实现了基于Hession2和Jdk的两种序列化实现)
    - 提供可配置化的缓存序列化方式(VdianRedis默认使用Hession2序列化, 相比Jdk性能更高, Guava本地缓存可以不用序列化)
    - ~~将VdianRedis连接池大小默认设置为16个连接, wait时间设置为10ms~~;
- 0.1.6
    - ~~开放VdianRedis连接池设置策略~~;

---
### 0.2.X
- 0.2.0
    - 添加基于通用Redis的`ICache`接口实现;
    - 添加基于Memcached的`ICache`实现;

---
### 0.3.X
- 0.3.0
    - `@Cached`/`@Invalidate`添加`cache`属性, 使cacher支持管理多个缓存实现
    - `@Cached`添加`condition`属性: 条件缓存, 支持SpEL表达式, 当表达式结果为`true`时缓存.
    - `@Cached`添加`prefix`属性, 为为当前方法的所有缓存添加统一前缀, 且支持无参方法缓存.
- 0.3.1
    - 添加EhCache缓存实现, 默认启用in-heap、off-heap、disk三级缓存
    - 添加MapDB缓存实现, 启用off-heap缓存
    - ~~添加VRedisClusterCache缓存实现, 启用微店Redis集群缓存~~
    - RedisCache: 基于`Pipeline`实现带有超时时间的`mset`命令, 提高性能.
- 0.3.2
    - fix `VdianRedisCluster`不支持多于100个key 读写的bug
    - 使用`ConcurrentHashMap`替换Commons-Pool2实现的ICachePool, 并对其进行代理(commons-proxy), 同样可以实现在指定缓存产品配错的情况下给出提示的功能.
- 0.3.3
    - fix Single Cache clean bug

---
### 0.4.X
- 0.4.0
    - 添加JMX支持(默认开启), 可以详细的查看缓存命中情况, 便于缓存优化, 提升系统吞吐:
        - 支持查看/重置**全部**key命中率
        - 支持查看/重置**分组**key命中率
- 0.4.1
    - fix 通用Redis mset 调用`exec()` bug, 改为`sync()`

---
### 0.5.X(设计 & 开发ing)
- 0.5.0
    - Cacher代码整理/解耦, 自定义IoC容器: 使`@Singleton`, `@Inject`生效
    - 消除限制4:
        - 提供以Map的`keySet`作为Multi的CacheKey参数支持.
    - 消除限制5:
        - 提供对`java.util.Collections.EmptyList`、`java.util.Collections.EmptyMap`等作为参数/返回值的支持(设计ing)
        - 提供对`java.util.Collections.CheckedList`、`java.util.Collections.CheckedMap`等作为参数/返回值的支持(设计ing)
        - 提供对`java.util.Collections.SingletonList`、`java.util.Collections.SingletonMap`等作为参数/返回值的支持(设计ing)
    - 添加`RedisClusterCache`, 提供对RedisCluster模式的支持.

---
### 0.6.X(todo)
- Cacher所有配置抽取为`Configurator`
- 定义Recorder接口, 抽象命中率统计
    - 实现基于ZooKeeper的命中率统计(以应用为单位, 重启历史数据不丢失)
    - 实现基于LevelDB的命中率统计(以机器为单位, 重启历史数据不丢失)
    - 实现基于ConcurrentHashMap的命中率统计(以及其为单位, 重启历史数据丢失)

---
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

---
## 入门
### 添加缓存
#### 1. pom.xml
```
<dependency>
  <groupId>com.vdian.cacher</groupId>
  <artifactId>cacher</artifactId>
  <version>0.3.3-SNAPSHOT</version>
</dependency>
```

---

#### 2. 注册(applicationContext.xml)
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans" ...>

    <!-- 启用自动代理: 如果已经开启则不必重复开启 -->
    <aop:aspectj-autoproxy/>

    <!-- 注入Cached切面:
            open: 定义Cacher的全局开关
            caches: 只要实现了ICache接口的cache产品均可被Cacher托管
     -->
    <bean class="CacherAspect">
        <constructor-arg name="open" value="true"/>
        <constructor-arg name="caches">
            <map key-type="java.lang.String" value-type="ICache">
                <entry key="noOp" value-ref="noOpCache"/>
                <entry key="jdkMap" value-ref="jdkConcurrentMapCache"/>
                <entry key="guava" value-ref="guavaCache"/>
                <entry key="mapDB" value-ref="mapDBCache"/>
                <entry key="ehcache" value-ref="ehCache"/>
                <!--
                    <entry key="vRedisPool" value-ref="vRedisPoolCache"/>
                    <entry key="vRedisCluster" value-ref="vRedisClusterCache"/>
                -->
                <entry key="redis" value-ref="redisCache"/>
                <entry key="memcached" value-ref="memcachedCache"/>
            </map>
        </constructor-arg>
    </bean>

    <!-- 接入cacher的缓存产品需要实现ICache 接口:
      默认提供了基于   无操作的缓存、
                     VdianRedisPool模式、
                     VdianRedisCluster模式、
                     Memcached、
                     通用Redis、
                     Guava、
                     JdkConcurrentHashMap、
                     MapDB、
                     Ehcache等9款缓存实现.(注意: 开源版本没有提供VdianPool模式和VdianCluster模式实现)
     -->
    <bean id="noOpCache" class="NoOpCache"/>
    <bean id="jdkConcurrentMapCache" class="JdkConcurrentMapCache"/>
    <bean id="guavaCache" class="GuavaCache">
        <constructor-arg name="size" value="1000"/>
        <constructor-arg name="expire" value="6000"/>
    </bean>
    <bean id="mapDBCache" class="MapDBCache">
        <constructor-arg name="interval" value="1000000"/>
        <constructor-arg name="maxSize" value="1000000"/>
        <constructor-arg name="maxStoreSize" value="2"/>
    </bean>
    <bean id="ehCache" class="EhCache">
        <constructor-arg name="heapEntries" value="20"/>
        <constructor-arg name="offHeapMBSize" value="512"/>
        <constructor-arg name="diskPath" value="/data/cache"/>
        <constructor-arg name="diskGBSize" value="1"/>
    </bean>
    <!--
        <bean id="vRedisPoolCache" class="com.vdian.cacher.support.cache.VRedisPoolCache">
            <constructor-arg name="namespace" value="ares"/>
        </bean>
        <bean id="vRedisClusterCache" class="com.vdian.cacher.support.cache.VRedisClusterCache">
            <constructor-arg name="namespace" value="feedcenter_cluster"/>
        </bean>
    -->
    <bean id="redisCache" class="RedisPoolCache">
        <constructor-arg name="host" value="10.1.101.60"/>
        <constructor-arg name="port" value="6379"/>
    </bean>
    <bean id="memcachedCache" class="MemcachedCache">
        <constructor-arg name="ipPorts" value="10.1.101.60:11211"/>
    </bean>

</beans>
```

---
#### 3. @Cached & @CacheKey 注解
- 在需要走缓存的方法前加`@Cached`注解
- 在想要组装为key的参数前添加`@CacheKey`注解
![](https://si.geilicdn.com/hz_img_0d8100000158bdf199220a02685e_1291_439_unadjust.png)

可以看到单key的方法已经只剩下了步骤2(省掉了步骤0、1、3), 多key的方法只剩下了步骤4(省掉了步骤0、1、2、3、5), 生成key、查询缓存、写入缓存的操作框架已经全部帮你完成了(而且你还省掉了一个生成key的`genCacheKey()`方法).

> 而从代码量上看: 基于单key的查询方法**由十四行减到了两行**, 而批量查询则更加恐怖的**从三四十行降到了三四行**, 而你所付出的成本, 则只是添加两个注解`@Cached`和`@CacheKey`, 以及简单的学习一下**cacher**这个框架(而如果缓存产品更换的话, 也只是需要根据目标缓存产品实现以下`ICache`接口, 然后在Spring的环境中配置一下就好了).

---
### 缓存失效
如果一个用户的信息(`User`)修改了怎么办? 按照原先的想法是 *在修改用户信息代码后添加缓存更新or缓存失效的功能, 在用户更新的同时让缓存失效或刷新* (说起来简单做起来繁琐, 就不再演示代码). 这种方法依然会导致在业务代码会嵌入大量跟缓存有关的内容, 而这一点**cacher**也帮你想到了 -`@Invalidate`注解:
![](https://si.geilicdn.com/hz_img_0c1f00000158f5b880e20a026860_1652_248_unadjust.png)
框架会在你的用户信息更新方法执行后去失效缓存.

> 提示: 想要失效一个缓存, 那肯定得能够找到这个缓存, 也就是失效缓存key的拼装规则一定要与添加缓存的规则一致(如prefix、separator等).


---
## cache统一日志
### 日志策略
- cacher会打印两种日志:
    - 读写缓存异常:
    由`CacheManager`控制, 读写缓存出错会打印日志, 且读缓存出错时会认为该key没有读到, 然后再次执行方法. 框架保证当缓存出现异常时不会影响正常的业务逻辑. (默认打印到`com.vdian.cacher` Logger和项目主Logger两个log内 -> **ERROR级别**);
    - 缓存执行统计: 每一次执行读写缓存会打印两条日志(打印到`com.vdian.cacher`Logger内: **INFO级别**)
        1. 缓存命中率: 见下图片. 单缓存还会打印查询的`key`, 批量缓存会打印 `miss keys`;
        2. 缓存执行耗时: 打印缓存执行总耗时(包含查询缓存、执行方法、写入缓存的总耗时).
![](https://si.geilicdn.com/hz_img_05aa00000158dc87d50e0a02685e_2022_453_unadjust.png)

> 可以看到在基于RedisPool的缓存中, 在命中率能够达到100%的情况下, 即使157个key, 也能做到在2ms内返回. 而单key则会在0ms内返回.

---
### 配置
> cacher使用SLF4J日志框架, 不与具体日志实现绑定, 因此很容易与项目内已有的日志实现配合使用. 如下是logback配置, log4j/log4j2类似:

```
<configuration>

    <property name="pattern" value="%d %p [%t] %c{20} %X{traceId:--} %m%n"/>

    <appender name="STD_OUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${pattern}</pattern>
        </encoder>
    </appender>

    <appender name="cacher" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/data/logs/cacher/cacher.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/data/logs/cacher/cacher.log.%d{yyyy-MM-dd}</fileNamePattern>
            <maxHistory>1</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${pattern}</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STD_OUT"/>
    </root>

    <logger name="com.vdian.cacher">
        <appender-ref ref="cacher"/>
    </logger>

</configuration>
```

---

## 缓存注解详解
> cacher一共提供三个注解`@Cached`、`@Invalidate`、`@CacheKey`.

---
### @Cached

> 在需要走缓存的方法前添加`@Cached`注解.

```
/**
 * @author jifang
 * @since 2016/11/2 下午2:22.
 */
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {

    /**
     * @return Specifies the <b>Used cache implementation</b>,
     * default the first {@code caches} config in {@code CacherAspect}
     * @since 0.3
     */
    String cache() default "default";

    /**
     * @return Specifies the start prefix on every key,
     * if the {@code Method} have non {@code param},
     * {@code prefix} is the <b>constant key</b> used by this {@code Method}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return use <b>SpEL</b>,
     * when this expression is {@code true}, this {@Code Method} will go through by cache
     * @since 0.3
     */
    String condition() default "";

    /**
     * @return expire time, time unit: <b>seconds</b>
     */
    int expire() default Expire.FOREVER;

    /**
     * @return multi part key`s separator
     * like: <i>part1-part2-part3</i>
     */
    String separator() default "-";
}
```

| 属性 | 描述 | Ext |
:-------: | ------- | ------- 
| `cache` | 指定使用的缓存产品, 值为`CacherAspect` `caches`参数的一个key | 选填: 默认为`default`, 即使用在`caches` Map的第一个Entry实例 |
| `prefix` | 缓存**key**的统一前缀 | 选填: 默认为`""`, 即不添加前缀, 若方法没有参数 or 方法没有`@CacheKey`注解, 则必须在此配置一个`prefix`, 另其成为一个该方法的***静态常量key***, 以后每次执行都走这个唯一的key |
| `condition` | SpEL表达式 | 选填: 默认为`""`, 即默认为`true`, 在Cacher执行前会首先计算该表达式的值, 只有当返回值为`true`时, 才会经过缓存, 在表达式执行前, Cacher会将方法的参数以`参数名` - `参数值`的**key-value**形式导入到表达式的环境中 |
| `expire` |  缓存过期时间, 单位秒 | 选填: 默认为`Expire.FOREVER` 永不过期, `Expire`提供了一些推荐值 |
| `separator` | 如果一个缓存Key由多个方法参数组成, 可由`separator`在中间作为分隔符 | 选填: 默认`-` |


---

### @Invalidate
> 在需要失效缓存的方法前添`@Invalidate`注解.

```
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invalidate {

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String cache() default "default";

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String condition() default "";

    /**
     * @return as {@code @Cached}
     */
    String separator() default "-";
}
```

-  `cache`、`prefix`、`condition`、`separator`与`@Cached`内含义相同, 只是`@Invalidate`少了一个失效时间属性(因为不需要).

---

### @CacheKey
> 在需要作为缓存key的方法参数前添加`@CacheKey`注解. 其是cacher的核心, 封装了Key的拼装/解析规则. ~~上面的两个注解必须有`@CacheKey`配合才能生效~~(0.3版本开始支持**常量key**缓存).

```
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheKey {

    String prefix() default "";

    /**
     * @return use a part of param as a cache key part
     */
    String expression() default "";

    /**
     * @return used when param is Collection instance,
     * read/write from/to ICache.multiRead/ICache.multiWrite
     */
    boolean multi() default false;

    /**
     * @return used when multi is true and method return Collection instance,
     * the method result is connected with that param
     */
    String identifier() default "";
}

```

| 属性 | 描述 |
:-------: | -------
| `prefix` | (选填: 默认为`""`) 指定Key的前缀, 目的是防止key冲突, 且便于在在后台查看缓存内容.  |
| `expression` | (选填: 默认为`""`) 一段SpEL表达式, 如果方法形参为一个`JavaBean`, 且只希望将该Bean的一个属性(或一部分内容)作为缓存的Key时, 指定一段SpEL表达式, 框架会在拼装缓存Key时解析该表达式以及传入的参数对象, 拿到你指定的某一部分. |
| `multi` | (选填: 默认为`false`) 指明该方法是否走批量缓存(例如走Redis的`mget`而非`get`), 其具体含义可参考**引入**部分的批量版本的`getFromDBOrHttp()`方法 |
| `identifier` | (选填: 默认为`""`) 也是一段SpEL表达式, `multi = true`时生效. 如果方法返回一个`Collection`实例, 需要由`identifier`来指定该`Collection`的单个元素的哪个属性与该`@CacheKey`参数关联 |


---
## 限制
### 1. `@CacheKey`为multi时, 入参list内的元素个数与返回值不一致:
```
@Cached
public List<FeedUser> invalidMulti(@CacheKey(multi = true, identifier = "id") List<Long> feedIds) {
   // ...
}
```

> 若以`(1,2,3)`调用`getFeedUsers()`方法, 却返回的是`{FeedUser(id=1), FeedUser(id=2), FeedUser(id=3), FeedUser(id=4)}`的对象列表(多一个id=4的对象), 框架只会缓存1、2、3的内容.

---

### ~~2. 没有`@CacheKey`注解~~
```
@Cached/@Invalidate
public String noneCacheKey(int id) {
   // ...
}
```
> 由于任何缓存都必须有一个key, 因此这种情况会在代码结构静态扫描时抛出异常, 提示开发者.
**从0.3版本开始, 支持无参单key函数**.

---
### 3. 多个@CacheKey属性为multi
```
@Cached
public Type multiMulti(@CacheKey(multi = true) List<Long> feedIds, @CacheKey(multi = true) List<Long> authorIds ) {
   // ...
}
```
> 如果多个`@CacheKey`属性为multi, 会导致方法的多个参数做笛卡尔积运算产生大量的key, 现阶段的框架还不能支持, 而且我们也不建议这样做(原因是这样很有可能会产生大量无用的缓存).因此这种情况会在代码结构静态扫描时抛出异常, 提示开发者注意.

---
### 4. 以Map作为multi

```
@Cached(namespace = "feedcenter")
public Type mapMulti(@CacheKey(multi) Map<Long, FeedUser> map) {
   // ...
}
```
> 如果将Map内所有的key-value拼装成key的话, 就会产生类似笛卡尔积的效果, 这种情况我们不建议. 不过如果是将Map.keySet作为拼装Key的列表的功能还是蛮常见的, 我们会在未来对这种情况支持.

---

### 5. 各类怪异的内部容器类调用
```
@Cached
public List<FeedUser> invalidCollection(@CacheKey(multi = true, identifier = "id") List<Long> feedIds) {
   // ...
}

// 以Arrays.asList()调用
invalidCollection(Arrays.asList(1L, 2L, 3L);
```

> 由于multi的Cache在构造容器返回值时需要反射调用容器类的构造方法, 但这些类并未提供公开的构造方法, 因此没法构造出对象, 这类容器有:

- Arrays.ArrayList
- Collections.SingleList

---
### 6. 缓存更新
框架现在还支持支持添加缓存和失效缓存两种操作, 暂时还不能支持缓存更新(但其实失效后再添加就是更新了O(∩_∩)O~).

---
- *by* 攻城师@翡青
    - Email: feiqing.zjf@gmail.com
    - 博客: [攻城师-翡青](http://blog.csdn.net/zjf280441589) - http://blog.csdn.net/zjf280441589
    - 微博: [攻城师-翡青](http://weibo.com/u/3319050953) - http://weibo.com/u/3319050953