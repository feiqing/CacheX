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
### 1.0.X
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
### 1.1.X
- 0.2.0
    - 添加基于通用Redis的`ICache`接口实现;
    - 添加基于Memcached的`ICache`实现;

---
### 1.2.X
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
### 1.3.X
- 0.4.0
    - 添加JMX支持(默认开启), 可以详细的查看缓存命中情况, 便于缓存优化, 提升系统吞吐:
        - 支持查看/重置**全部**key命中率
        - 支持查看/重置**分组**key命中率
- 0.4.1
    - fix 通用Redis mset 调用`exec()` bug, 改为`sync()`

---
### 1.4.X(设计 & 开发ing)
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
### 1.5.X(todo)
- Cacher所有配置抽取为`Configurator`
- 定义Recorder接口, 抽象命中率统计
    - 实现基于ZooKeeper的命中率统计(以应用为单位, 重启历史数据不丢失)
    - 实现基于LevelDB的命中率统计(以机器为单位, 重启历史数据不丢失)
    - 实现基于ConcurrentHashMap的命中率统计(以及其为单位, 重启历史数据丢失)


---
### 2.0.X(目标)
