# CacheX 注解缓存框架
> 1.7.3-Hessian-SNAPSHOT

- 目前已接入10+应用, 欢迎同学们使用并提出宝贵建议~~

---

## I. 架构
![架构模型](https://img.alicdn.com/tfs/TB1KPtTusfpK1RjSZFOXXa6nFXa-756-731.png)

---
## II. 简单使用
### 配置
- pom
```xml
<dependency>
    <groupId>com.github.cachex</groupId>
    <artifactId>cachex</artifactId>
    <version>${cachex.version}</version>
</dependency>
```
- XML注册
```xml
<!-- 启用自动代理: 如果已经开启则不必重复开启 -->
<aop:aspectj-autoproxy proxy-target-class="true"/>

<!-- 配置CacheX切面(CacheX代理需要手动生效) -->
<bean id="cachex" class="com.github.cachex.CacheXAspect">
    <constructor-arg name="caches">
        <map>
            <entry key="default" value-ref="guava"/>
            <entry key="tair" value-ref="tair"/>
            <entry key="redis" value-ref="redis"/>
        </map>
    </constructor-arg>
</bean>

<!-- com.github.cachex.ICache 接口实现 -->
<bean id="guava" class="com.github.cachex.support.cache.GuavaCache">
    <constructor-arg name="expire" value="600000"/>
    <constructor-arg name="size" value="100000"/>
</bean>

<bean id="tair" class="com.github.cachex.support.cache.TairCache">
    <constructor-arg name="tairManager" ref="tairManager"/>
    <constructor-arg name="namespace" value="${engine.tair.namespace}"/>
</bean>

<bean id="redis" class="com.github.cachex.support.cache.RedisPoolCache">
    <constructor-arg name="jedisPool" ref="jedisPool"/>
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
> CacheX提供如下注解`@Cached`、`@Invalid`、`@CacheKey`.
(ext: `@CachedGet`、`@CachedWrite`)

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
    String value() default "";

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
| `value` | 指定缓存实现: `CacheXAspect`/`CacheXProxy`的`caches`参数的key | 选填: 默认为注入caches的第一个实现(即`caches`的第一个Entry实例) |
| `prefix` | 缓存**key**的统一前缀 | 选填: 默认为`""`, 若方法无参或没有`@CacheKey`注解, 则必须在此配置一个`prefix`, 令其成为***缓存静态常量key*** |
| `condition` | SpEL表达式 | 选填: 默认为`""`(`true`), 在CacheX执行前会先eval该表达式, 当表达式值为`true`才会执行缓存逻辑 |
| `expire` |  缓存过期时间(秒) | 选填: 默认为`Expire.FOREVER` | 


---

### @Invalid
- 在需要失效缓存的方法前添加`@Invalid`注解.

```java
@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invalid {

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String value() default "";

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
> 注解内属性含义与`@Cached`相同.

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
     * @return used multi model(value has `#i` index) and method return {@code Collection},
     * the {@code field} indicate which of the {@code Collection}'s entity field related with this param
     */
    String field() default "";
}
```

| 属性 | 描述 | Ext |
:-------: | ------- | -------
| `value` | SpEL表达式: 缓存key的拼装逻辑 | 选填: 默认为`""` |
| `field` | **批量模式**(`value`参数包含`#i`索引)且方法返回值为`Collection`时生效: 指明该返回值的某个属性是与该参数是关联起来的 | 详见Ext.批量模式 |


---

### Ext. @CachedGet
- 在需要走缓存的方法前添加`@CachedGet`注解.
> 与`@Cached`的不同在于`@CachedGet`只会从缓存内查询, 不会写入缓存(当缓存不存在时, 只是会取执行方法, 但不讲方法返回内容写入缓存).

```java
@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedGet {

    /**
     * @return Specifies the <b>Used cache implementation</b>,
     * default the first {@code caches} config in {@code CacheXAspect}
     * @since 0.3
     */
    String value() default "";

    /**
     * @return Specifies the start keyExp on every key,
     * if the {@code Method} have non {@code param},
     * {@code keyExp} is the <b>constant key</b> used by this {@code Method}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return use <b>SpEL</b>,
     * when this spel is {@code true}, this {@Code Method} will go through by cache
     * @since 0.3
     */
    String condition() default "";
}
```

> 注解内属性含义与`@Cached`相同.

---

### Ext. @CachedWrite
- 在需要写缓存的方法前添加`@CachedGet`注解.
> 与`@Cached`的不同在于`@CachedWrite`只会将方法执行返回值写入缓存, 但不会从缓存中加载数据(todo: 待实现).

---
### Ext. 批量模式

![](https://img.alicdn.com/tfs/TB16uFgu8jTBKNjSZFNXXasFXXa-1042-133.png)

在该模式下: `#i`指定了ids作为批量参数: 假设ids={1,2,3}, CacheX会结合前面的prefix组装出 {`[USER]:1`、`[USER]:2`、`[USER]:3`} 这3个key去批量的查询缓存, 假设只有{1,2}能够命中, 则CacheX会只保留{3}去调用`getUsers()`方法, 将返回值写入缓存后, 将两部分内容进行merge返回.

1. 注意1: 如果方法的返回值为`Collection`实例: 则`@CacheKey`必须指定`field`参数, 该参数会指定`Collection`元素(如`User`)内的某个属性(如`id`)与批量参数的元素(如`ids`内的元素项)是一一对应的, 这样CacheX就可以根据该属性提取出参数值, 拼装key然后写入缓存.
2. 注意2. 如果方法的返回值为`Map`实例: 则`field`属性不填, 默认使用Map的Key作为`field`.
3. 注意3. `#i`作为批量模式指示器, 批量模式需要使用`#i`来开启, `#i`指明某个参数作为批量参数, CacheX会不断的迭代该参数生成批量缓存key进行缓存的读写.

---
### Ext. SpEL执行环境
对于`@CacheKey`内的`value`属性(SpEL), CacheX在将方法的参数组装为key时, 会将整个方法的参数导入到SpEL的执行环境内,
所以在任一参数的`@CacheKey`的`value`属性内都可以自由的引用这些变量, 如:
![](https://img.alicdn.com/tfs/TB1Mza0pcj_B1NjSZFHXXaDWpXa-1039-529.png)
尽管在`arg0`我们可以引用整个方法的任意参数, 但为了可读性, 我们仍然建议对某个参数的引用放在该参数自己的`@CacheKey`内
![](https://img.alicdn.com/tfs/TB1U23qn7omBKNjSZFqXXXtqVXa-1206-440.png)

> 注意: 在Java8环境中, 如果编译时没有指定`-parameters`参数, 则参数名默认为`arg0`、`arg1`、...、`argN`, 如果指定了该参数, 则在`spel`中使用实际的参数名即可, 如:`#source.name()`; 为了兼容这两种方式, CacheX提供了自己的命名方式`args0`、`args1`、...、`argsN`, 使用户可以不用区分是否开启编译参数.

### Ext. CacheX当前默认支持的缓存实现
![](https://img.alicdn.com/tfs/TB1JF82uwHqK1RjSZJnXXbNLpXa-259-231.png)
