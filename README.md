# cacher 声明式注解缓存框架(1.5.x版本)

> 愿景: 让你的代码中没有一行缓存相关代码!!!
- 目前已接入10+应用, 欢迎同学们使用并给出宝贵建议~~

- [版本历史](./markdown/release.md)
- [下一里程碑版本目标](./markdown/target.md)
- [why cacher?](markdown/whycachex.md)
- [命中率分组统计](./markdown/shooting.md)
- [使用限制](./markdown/limit.md)
- [dependency](dependency.txt)

---
>  原服务于`feedcenter`动态中心的 ***redis-annotation*** Redis注解框架重构:
- 0.X版本每天提供动态中心系统三个应用350W+次Dubbo调用, 2.6亿+次缓存读写, 单次查询(get/mget)耗时 0~2ms (1~200个key);
- 1.0版本: 框架重构, 不再与具体缓存产品绑定, 提供更灵活的配置、更快的读写效率;
- 1.3版本: 提供基于JMX暴露的分布命中率统计, 可以针对某一具体业务场景进行缓存&业务逻辑优化;
- 1.4版本: 添加`TairCache`实现, 支持Tair **MDB/LDB**开箱即用(优化: 支持对象无`Serializable`接口).

---

## I. 架构
![](http://7xrgh9.com1.z0.glb.clouddn.com/17-11-15/26560785.jpg)

---
## II. 简单使用
### 引入
- pom.xml
```xml
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
                caches: 只要实现了ICache接口的cache产品均可被Cacher托管
         -->
        <bean class="com.alibaba.cacher.CacherAspect">
            <constructor-arg name="caches">
                <map key-returnType="java.lang.String" value-returnType="com.alibaba.cacher.ICache">
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

---

### 使用
#### 1. 添加缓存(`@Cached` & `@CacheKey`)
- 在想要添加缓存的方法上标`@Cached`注解
- 在想要组装为key的方法参数上标`@CacheKey`注解
![](http://7xrgh9.com1.z0.glb.clouddn.com/17-11-15/37122658.jpg)

可以看到单key的方法已经只剩下了步骤2(省掉了步骤0、1、3), 多key的方法只剩下了步骤4(省掉了步骤0、1、2、3、5);
生成key、查询缓存、写入缓存的操作框架已经全部帮你完成了(而且还帮你省掉了一个生成key的`genCacheKey()`方法).
> 仅从代码量上看: 基于单key的查询方法**由14行减到了2行**, 而批量查询则更加恐怖的**从30/40行降到了3/4行**, 而你所付出的成本, 则只是添加两个注解`@Cached`和`@CacheKey`.
(附: 具体步骤编号可参考[why cacher?](markdown/whycachex.md))

---
#### 2. 缓存失效(`@Invalid` & `@CacheKey`)
如果一个用户的信息(`User`)修改了怎么办? 按照原先的想法是 *在修改用户信息代码后添加缓存更新/失效的逻辑, 在用户更新的同时让缓存刷新/失效*. 这种方法依然会导致在业务逻辑中充斥大量跟缓存有关的逻辑, 极不优雅!

![](http://7xrgh9.com1.z0.glb.clouddn.com/17-11-15/41363097.jpg)

框架会在你的用户信息更新方法执行后去自动的失效缓存.

> 提示: 想要失效一个缓存, 那肯定得能够找到这个缓存, 也就是失效缓存key的拼装规则一定要与添加缓存的规则一致(如`prefix`、`separator`等).

---
## III. 注解详解
> cacher一共提供三个注解`@Cached`、`@Invalid`、`@CacheKey`.

---
### @Cached

> 在需要走缓存的方法前添加`@Cached`注解.

```java
/**
 * @author jifang
 * @since 2016/11/2 下午2:22.
 */
@Documented
@Repeatable(Cacheds.class)
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {

    /**
     * @return Specifies the <b>Used cache implementation</b>,
     * default the first {@code caches} config in {@codeCacheXAspect}
     * @since 0.3
     */
    String cache() default "";

    /**
     * @return Specifies keyExp
     * if the {@code Method} have non {@code param},
     * {@codekeyExp} is the <b>constant key</b> used by this {@code Method}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return use <b>SpEL</b>,
     * when this spel is {@code true}, this {@Code Method} will go through by cache
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
| `cache` | 指定使用的缓存产品, 值为`CacherAspect.caches`参数的一个key | 选填: 默认为注入Cacher的第一个缓存实现, 即在`caches` Map的第一个Entry实例 |
| `prefix` | 缓存**key**的统一前缀 | 选填: 默认为`""`, 即不添加前缀, 若方法没有参数 or 方法没有`@CacheKey`注解, 则必须在此配置一个`prefix`, 令其成为该方法的***静态常量key***, 以后每次执行都走这个唯一的key |
| `condition` | SpEL表达式 | 选填: 默认为`""`, 即默认为`true`, 在Cacher执行前会首先计算该表达式的值, 只有当返回值为`true`时, 才会经过缓存, 在表达式执行前, Cacher会将方法的参数以`参数名` - `参数值`的**key-value**形式导入到表达式的环境中 |
| `expire` |  缓存过期时间, 单位秒 | 选填: 默认为`Expire.FOREVER` 永不过期, `Expire`提供了一些推荐值 |
| `separator` | 如果一个缓存Key由多个方法参数组成, 可由`separator`在中间作为分隔符 | 选填: 默认`-` |


---

### @Invalid
> 在需要失效缓存的方法前添`@Invalid`注解.

```java
/**
 * @author jifang
 * @since 16/7/19 下午4:21.
 */
@Documented
@Repeatable(Invalids.class)
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invalid {

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String cache() default "";

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

-  `cache`、`prefix`、`condition`、`separator`与`@Cached`内含义相同, 只是`@Invalid`少了一个失效时间属性(因为不需要).

---

### @CacheKey
> 在需要作为缓存key的方法参数前添加`@CacheKey`注解. `@CacheKey`内封装了Key的拼装/解析规则:

```java
/**
 * @author jifang
 * @since 16/7/19 下午6:07.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheKey {

    String prefix() default "";

    /**
     * @return use a part of param as a cache key part
     */
    String spel() default "";

    /**
     * @return used when param is Collection instance,
     * read/write from/to ICache.multiRead/ICache.multiWrite
     */
    boolean multi() default false;

    /**
     * @return used when multi is true and method return Collection instance,
     * the method result is connected with that param
     */
    String id() default "";
}


```

| 属性 | 描述 | Ext |
:-------: | ------- | -------
| `prefix` | (选填: 默认为`""`) 指定Key的前缀, 目的是防止key冲突, 且便于在在后台查看缓存内容.  | |
| `spel` | (选填: 默认为`""`) 一段SpEL表达式, 如果方法形参为一个`JavaBean`, 且只希望将该Bean的一个属性(或一部分内容)作为缓存的Key时, 指定一段SpEL表达式, 框架会在拼装缓存Key时解析该表达式以及传入的参数对象, 拿到你指定的某一部分. | 曾经见过的高级用法`spel="'id:'+id+'-name:'+name+'-address:'+getAddress()+'-time:'+getBirthday()"` |
| `multi` | (选填: 默认为`false`) 指明该方法是否走批量缓存(如调用Redis的`mget`而非`get`), 其具体含义可参考**why cacher**部分的批量版本的`getFromDBOrHttp()`方法 |
| `id` | (选填: 默认为`""`) 也是一段SpEL表达式, `multi = true`时生效. 如果方法返回一个`Collection`实例, 需要由`id`来指定该`Collection`的单个元素的哪个属性与该`@CacheKey`参数关联 | |


---
### 附

cacher目前版本支持的**缓存产品实现**、**序列化类型**、**命中率统计实现**们 ~

![support.png](http://7xrgh9.com1.z0.glb.clouddn.com/17-11-15/58610258.jpg) 

---
- *by* 菜鸟-翡青
    - Email: jifang.zjf@alibaba-inc.com
    - 博客: [菜鸟-翡青](http://blog.csdn.net/zjf280441589) - http://blog.csdn.net/zjf280441589
    - 微博: [菜鸟-翡青](http://weibo.com/u/3319050953) - http://weibo.com/u/3319050953