# cacher 声明式注解缓存框架(1.5.X版本)

标签 ： 开源

---

>  原服务于`feedcenter`动态中心的 ***redis-annotation*** Redis注解框架重构:
- 0.X版本每天提供动态中心系统三个应用350W+次Dubbo调用, 2.6亿+次缓存读写, 单次查询(get/mget)耗时 0~2ms (1~200个key);
- 1.0版本: 框架重构, 不再与具体缓存产品绑定, 提供更灵活的配置、更快的读写效率;
- 1.3版本: 提供基于JMX暴露的分布命中率统计, 可以针对某一具体业务场景进行缓存&业务逻辑优化;
- 1.5版本: 添加`TairCache`实现, Tair缓存开箱即用.

- [版本历史](./markdown/release.md)
- [why cacher?](./markdown/whycacher.md)

---

## I. 架构
![](https://private-alipayobjects.alipay.com/alipay-rmsdeploy-image/skylark/png/16257/370eee6562be41fa.png)

---
## II. 简单使用
### 引入
- pom.xml
```
<dependency>
  <groupId>com.alibaba.cacher</groupId>
  <artifactId>cacher</artifactId>
  <version>1.5.2-SNAPSHOT</version>
</dependency>
```

- Spring Bean注册(applicationContext.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       ...>

        <!-- 启用自动代理: 如果已经开启则不必重复开启 -->
        <aop:aspectj-autoproxy/>
    
        <!-- 注入Cacher切面:
                open: 定义Cacher的全局开关
                caches: 只要实现了ICache接口的cache产品均可被Cacher托管
         -->
        <bean class="com.alibaba.cacher.CacherAspect">
            <constructor-arg name="caches">
                <map key-type="java.lang.String" value-type="com.alibaba.cacher.ICache">
                    <entry key="tair" value-ref="tair"/>
                </map>
            </constructor-arg>
        </bean>
    
        <bean id="tair" class="com.alibaba.cacher.support.cache.TairCache" lazy-init="true">
            <constructor-arg name="configId" value="mdbcomm-daily"/>
            <constructor-arg name="namespace" value="180"/>
        </bean>
</beans>
```

> 

---

### 使用
#### 添加缓存(`@Cached` & `@CacheKey`)
- 在想要添加缓存的方法上标`@Cached`注解
- 在想要组装为key的方法参数上标`@CacheKey`注解
![](https://si.geilicdn.com/hz_img_0d8100000158bdf199220a02685e_1291_439_unadjust.png)

可以看到单key的方法已经只剩下了步骤2(省掉了步骤0、1、3), 多key的方法只剩下了步骤4(省掉了步骤0、1、2、3、5), 生成key、查询缓存、写入缓存的操作框架已经全部帮你完成了(而且你还省掉了一个生成key的`genCacheKey()`方法).
> 具体步骤可参考[why cacher?](./markdown/whycacher.md)

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
- *by* 菜鸟-翡青
    - Email: jifang.zjf@alibaba-inc.com
    - 博客: [菜鸟-翡青](http://blog.csdn.net/zjf280441589) - http://blog.csdn.net/zjf280441589
    - 微博: [菜鸟-翡青](http://weibo.com/u/3319050953) - http://weibo.com/u/3319050953