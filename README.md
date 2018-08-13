# CacheX 注解缓存框架
> 1.7.1-SNAPSHOT

- 目前已接入10+应用, 欢迎同学们使用并提出宝贵建议~~

---

## I. 架构
![架构模型](https://img.alicdn.com/tfs/TB1UooVn8jTBKNjSZFDXXbVgVXa-1206-1324.png)

---
## II. 简单使用
### 配置
- pom
```xml
<dependency>
      <groupId>com.github.cachex</groupId>
      <artifactId>cachex</artifactId>
      <version>1.7.1-SNAPSHOT</version>
</dependency>
```
- Spring注册
```xml
<!-- 启用自动代理: 如果已经开启则不必重复开启 -->
<aop:aspectj-autoproxy proxy-target-class="true"/>

<!-- 配置CacheX切面 -->
<bean class="com.github.cachex.CacheXAspect">
    <constructor-arg name="caches">
        <map>
            <entry key="redis" value-ref="redisCache"/>
        </map>
    </constructor-arg>
</bean>

<!-- com.github.cachex.ICache 接口实现 -->
<bean id="redisCache" class="com.github.cachex.support.cache.RedisCache">
    <constructor-arg name="host" value="${redis.server.host}"/>
    <constructor-arg name="port" value="${redis.server.port}"/>
</bean>
```

---

### 使用
#### 1. 添加缓存(`@Cached` & `@CacheKey`)
- 在要添加缓存的方法上标`@Cached`
- 在要组装为key的方法参数上标`@CacheKey`

![](https://img.alicdn.com/tfs/TB1QQd4n26TBKNjSZJiXXbKVFXa-626-144.png)

---
#### 2. 缓存失效(`@Invalid` & `@CacheKey`)
![](https://img.alicdn.com/tfs/TB1FyI2n5AnBKNjSZFvXXaTKXXa-631-111.png)

---
## III. 注解详解
> CacheX一共提供三个注解`@Cached`、`@Invalid`、`@CacheKey`.

---
### @Cached
- 在需要走缓存的方法前添加`@Cached`注解.

```java
/**
 * @author jifang
 * @since 2016/11/2 下午2:22.
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {

    /**
     * @return Specifies the <b>Used cache implementation</b>,
     * default the first {@code caches} config in {@code CacheXAspect}
     */
    String cache() default "";

    /**
     * @return Specifies the start prefix on every key,
     * if the {@code Method} have non {@code param},
     * {@code prefix} is the <b>constant key</b> used by this {@code Method}
     */
    String prefix() default "";

    /**
     * @return use <b>SpEL</b>,
     * when this spel is {@code true}, this {@code Method} will go through by cache
     */
    String condition() default "";

    /**
     * @return expire time, time unit: <b>seconds</b>
     */
    int expire() default Expire.FOREVER;
}
```

| 属性 | 描述 | Ext |
:-------: | ------- | ------- 
| `cache` | 指定缓存产品, 值为`caches`参数的一个key | 选填: 默认为注入CacheX的第一个实现(即`caches`的第一个Entry实例) |
| `prefix` | 缓存**key**的统一前缀 | 选填: 默认为`""`, 若方法没有参数或没有`@CacheKey`注解, 则必须在此配置一个`prefix`, 令其成为***静态常量key*** |
| `condition` | SpEL表达式 | 选填: 默认为`""`(`true`), 在CacheX执行前会首先计算该表达式的值, 只有当返回值为`true`时, 才会经过缓存, 在表达式执行前, CacheX会将方法的参数以`参数名` - `参数值`的**key-value**形式导入到表达式的环境中 |
| `expire` |  缓存过期时间(秒) | 选填: 默认为`Expire.FOREVER` | 

---

### @Invalid
- 在需要失效缓存的方法前添`@Invalid`注解.

```java
/**
 * @author jifang
 * @since 16/7/19 下午4:21.
 */
@Documented
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
}
```
> 属性含义与`@Cached`相同.

---

### @CacheKey
- 在需要作为缓存key的方法参数前添加`@CacheKey`注解.

```java
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheKey {

    /**
     * @return use a part of param as a cache key part
     */
    String value() default "";
    
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
| `value` | 一段SpEL表达式 | 选填: 默认为`""` |
| `multi` | 指明该方法是否走批量缓存(如调用Redis的`mget`而非`get`), 其具体含义可参考**why cachex**部分的批量版本的`getFromDBOrHttp()`方法 | 选填: 默认为`false` |
| `id` | `multi = true`时生效, 指明该参数与返回值的哪个属性相关联. |  如果方法返回一个`Collection`实例, 需要由`id`来指定该`Collection`的单个元素的哪个属性与该`@CacheKey`参数关联, 选填: 默认为`""` |

### SpEL执行环境
- 如果方法形参为一个`JavaBean`, 且只希望将该Bean的一个属性(或一部分内容)作为缓存的Key时, 指定一段SpEL表达式, 框架会在拼装缓存Key时解析该表达式以及传入的参数对象, 拿到你指定的某一部分.

![](https://img.alicdn.com/tfs/TB1U23qn7omBKNjSZFqXXXtqVXa-1206-440.png)

---
### 附

CacheX目前版本支持的**缓存产品实现**、**序列化类型**、**命中率统计实现**们 ~

![support.png](http://7xrgh9.com1.z0.glb.clouddn.com/17-11-15/58610258.jpg) 

## link
- [版本历史](./markdown/release.md)
- [下一里程碑版本目标](./markdown/target.md)
- [why cachex?](markdown/whycachex.md)
- [命中率分组统计](./markdown/shooting.md)
- [使用限制](./markdown/limit.md)

## 应用场景
>  原服务于`feedcenter`动态中心的 ***redis-annotation*** Redis注解框架重构:
- 0.X版本每天提供动态中心系统三个应用350W+次Dubbo调用, 2.6亿+次缓存读写, 单次查询(get/mget)耗时 0~2ms (1~200个key);
- 1.0版本: 框架重构, 不再与具体缓存产品绑定, 提供更灵活的配置、更快的读写效率;
- 1.3版本: 提供基于JMX暴露的分布命中率统计, 可以针对某一具体业务场景进行缓存&业务逻辑优化;
- 1.4版本: 添加`TairCache`实现, 支持Tair **MDB/LDB**开箱即用(优化: 支持对象无`Serializable`接口).

---
- *by* 菜鸟-翡青
    - Email: jifang.zjf@alibaba-inc.com
    - 博客: [菜鸟-翡青](http://blog.csdn.net/zjf280441589) - http://blog.csdn.net/zjf280441589
    - 微博: [菜鸟-翡青](http://weibo.com/u/3319050953) - http://weibo.com/u/3319050953