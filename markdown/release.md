## 重大版本历史

### 0.0.x
- 最初的redis-annotation, 作为一个RedisClusterClient的封装提供给如下应用使用
    - feedcenter
    - feedcenter-push
    - feedcenter-admin
- 文档
    - [redis-annotation 文档](https://github.com/feiqing/CacheX/wiki/redisCli-annotation-%E6%96%87%E6%A1%A3)
    - [redis-annotation 分享](https://github.com/feiqing/CacheX/wiki/redisCli-annotation-%E5%88%86%E4%BA%AB)
- 性能要求
    - 动态中心系统三个应用350W+次Dubbo调用, 2.6亿+次缓存读写, 单次查询(get/mget)耗时 0~2ms (1~200个key);

---
### 1.0.x(0.1.x)
- 1.0.0
    - 重构为CacheX: 开放`ICache`接口, 不再强依赖某一特定缓存产品, 而是作为`ICache`接口的一个具体实现, 以支持更多的缓存服务(如Memcached、Redis、Guava、`ConcurrentHashMap`...);
- 1.0.1
    - 添加全局缓存开关`cachex` Switch, 支持CacheX动态开关;
- 1.0.2
    - 开放`com.github.cachex.ISerializer`缓存序列化接口;
   
---
### 1.2.x(0.3.x)
- `@Cached`/`@Invalidate`添加`value`属性, 使CacheX支持管理多个缓存实现;
- `@Cached`添加`condition`属性: 条件缓存, 支持SpEL表达式, 当表达式结果为`true`时缓存;
- `@Cached`添加`prefix`属性, 为为当前方法的所有缓存添加统一前缀, 且支持无参方法缓存;

---
### 1.3.x
- 支持缓存命中率分组统计功能, 添加JMX支持, 可以详细查看各类业务缓存命中情况, 便于缓存优化, 提升系统吞吐:
    - 支持查看/重置**全部**key命中率;
    - 支持查看/重置**分组**key命中率;

---
### 1.4.x
- CacheX部分配置代码重构, 自定义IoC容器: 使`@Singleton`, `@Inject`生效;

---
### 1.5.x
- 1.5.1
    - 抽象并开放出`com.github.cachex.ShootingMXBean`接口, 支持自定义缓存分组命中率统计实现;
    - 添加`MemoryShootingMXBeanImpl`, 支持基于内存计数的缓存命中率统计((以机器为单位, 重启历史数据丢失));
- 1.5.2
    - 添加`DerbyShootingMXBeanImpl`、`H2ShootingMXBeanImpl`实现, 支持基于嵌入式DB的缓存命中率统计(以机器为单位, 重启历史数据不丢失; 其中Derby实现可以动态加载jdk提供的derby.jar包, 实现0依赖配置)
    - 添加`ZKShootingMXBeanImpl`实现, 支持基于ZooKeeper的异步命中率统计, 可以做到统一应用共享计数器(以应用为单位, 重启历史数据不丢失);
- 1.5.4
    - 消除[限制4](limit.md#4-各类怪异的内部容器类调用), 支持:
    
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
- 1.5.5
    - 删除`@Cached`/`@Invalid`/`@CachedGet`内的`seperator`属性, 似缓存key拼装更简单

### 1.6.x
- 添加缓存防击穿策略(开启后: 如果执行方法返回`null`, 则向缓存内写入一个空对象, 下次不走DB)
- `ISerializer`抽取为`com.github.jbox.serializer.ISerializer`, 放入jbox内, CacheX开始依赖jbox-1.6

### 1.7.x
- 将`CacheXAspect`拆分成为`CacheXAspect`与`CacheXCore`, 并添加`CacheXProxy`, 
使CacheX的核心保持在`CacheXCore`至`ICache`以上:
    - 在`CacheXCore`以上可以任意添加框架的门面;
    - 在`ICache`以下可以任意添加缓存服务的实现;
- 将自研IoC替换为Google Guice.
- 专注于稳定、高性能的缓存框架.

---