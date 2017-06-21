## 版本历史
### 0.0.x
- 最初的redis-annotation, 作为一个RedisClusterClient的封装提供给如下应用使用
    - feedcenter
    - feedcenter-push
    - feedcenter-admin
- 文档
    - [redis-annotation 文档](https://github.com/feiqing/Cacher/wiki/redisCli-annotation-%E6%96%87%E6%A1%A3)
    - [redis-annotation 分享](https://github.com/feiqing/Cacher/wiki/redisCli-annotation-%E5%88%86%E4%BA%AB)

---
### 1.0.x(0.1.x)
- 1.0.0
    - 重构为cacher
        - 开放`ICache`接口, 不再强依赖某一特定缓存产品, 而是作为`ICache`接口的一个具体实现, 以支持更多的缓存服务(如Memcached、Redis、Guava、`ConcurrentHashMap`...);
    - 添加`NoOpCache`无操作缓存;
    - 添加`GuavaCache`的LocalCache实现;
    - 添加`RedisPoolCache`作为基于Pool模式Redis单机缓存实现
    - 添加`RedisClusterCache`作为Redis分布式缓存实现;
- 1.0.1
    - 添加全局缓存开关`open`参数, 支持Cacher动态开关;
- 1.0.2
    - 开放`com.alibaba.cacher.IObjectSerializer`缓存序列化接口;
    - 提供基于Hession2和Jdk的两种序列化实现;
- 1.0.3
    - 开放`RedisPoolCache`连接池设置策略;

---
### 1.1.x
- 1.1.0
    - 添加`JdkConcurrentMapCache`内存缓存实现;
    - 添加`MemcachedCache`基于Memcached的缓存实现;

---
### 1.2.x(0.3.x)
- 1.2.0
    - `@Cached`/`@Invalidate`添加`cache`属性, 使cacher支持管理多个缓存实现;
    - `@Cached`添加`condition`属性: 条件缓存, 支持SpEL表达式, 当表达式结果为`true`时缓存;
    - `@Cached`添加`prefix`属性, 为为当前方法的所有缓存添加统一前缀, 且支持无参方法缓存;
- 1.2.1
    - 添加`EhCache`基于Ehcache缓存实现, 默认启用***in-heap***、***off-heap***、***disk***三级缓存;
    - 添加`MapDBCache`, 专用的***off-heap***缓存实现;
    - `RedisPoolCache`改进: 基于`Pipeline`实现带有超时时间的`mset`命令, 提高性能;
- 1.2.2
    - 使用`ConcurrentHashMap`替换Commons-Pool2实现的ICachePool, 并对其进行代理(commons-proxy), 同样可以实现在指定缓存产品配错的情况下给出提示的功能;
- 1.2.3
    - fix Single Cache clean bug;

---
### 1.3.x
- 1.3.0
    - 支持缓存命中率分组统计功能, 添加JMX支持, 可以详细查看各类业务缓存命中情况, 便于缓存优化, 提升系统吞吐:
        - 支持查看/重置**全部**key命中率;
        - 支持查看/重置**分组**key命中率;
- 1.3.1
    - fix `RedisClusterCache` `mset` 调用`exec()` bug, 改为`sync()`;

---
### 1.4.x
- 1.4.0
    - Cacher部分配置代码重构, 自定义IoC容器: 使`@Singleton`, `@Inject`生效;
    - 添加`TairCache`缓存实现, 支持Tair **MDB/LDB**开箱即用;
    - 添加`LevelDBCache`缓存实现, 集成LevelDB的高性能大数据量写入;

---
### 1.5.x
- 1.5.1
    - 抽象并开放出`com.alibaba.cacher.ShootingMXBean`接口, 支持自定义缓存分组命中率统计实现;
    - 添加`MemoryShootingMXBeanImpl`, 支持基于内存计数的缓存命中率统计((以机器为单位, 重启历史数据丢失));
- 1.5.2
    - 添加`DerbyShootingMXBeanImpl`、`H2ShootingMXBeanImpl`实现, 支持基于嵌入式DB的缓存命中率统计(以机器为单位, 重启历史数据不丢失; 其中Derby实现可以动态加载jdk提供的derby.jar包, 实现0依赖配置)
    - 添加`ZKShootingMXBeanImpl`实现, 支持基于ZooKeeper的异步命中率统计, 可以做到统一应用共享计数器(以应用为单位, 重启历史数据不丢失);
    - 添加`@Cacheds`、`@Invalids`两个注解, 使`@Cached`、`@Invalid`支持Java8重复注解, 定义多级缓存目标.
- 1.5.3
    - 添加`com.alibaba.cacher.support.serialize.KryoSerializer`序列化实现
- 1.5.4
    - 消除限制4, 支持:
    
    | Map | Collection | 
    :------- | ------- |
    | `Collections.emptyMap()` | `Collections.emptyList()` |
    | `Collections.emptyNavigableMap()` | `Collections.emptySet()` | 
    | `Collections.emptySortedMap()` | `Collections.emptySortedSet()` |
    | `Collections.singletonMap()`   | `Collections.emptyNavigableSet()` |
    | `Collections.unmodifiableMap()` | `Collections.singletonList()` |
    | `Collections.unmodifiableNavigableMap()` | `Collections.singleton()` |
    | `Collections.unmodifiableSortedMap()` | `Arrays.asList()` | 
    | `Collections.synchronizedMap()` | `Collections.unmodifiableCollection()` |
    | `Collections.synchronizedNavigableMap()` | `Collections.unmodifiableList()` |
    | `Collections.synchronizedSortedMap()` | `Collections.unmodifiableSet()` |
    | `Collections.checkedMap()` | `Collections.unmodifiableSortedSet()` |
    | `Collections.checkedNavigableMap()` | `Collections.unmodifiableNavigableSet()` |
    | `Collections.checkedSortedMap()` | `Collections.synchronizedCollection()` |
    | | `Collections.synchronizedList()` |
    | | `Collections.synchronizedSet()` |
    | | `Collections.synchronizedNavigableSet()` |
    | | `Collections.synchronizedSortedSet()` |
    | | `Collections.checkedCollection()` |
    | | `Collections.checkedList()` |
    | | `Collections.checkedSet()` |
    | | `Collections.checkedNavigableSet()` |
    | | `Collections.checkedSortedSet()` |
    | | `Collections.checkedQueue()` |

---