# TODO: 目标成为一个缓存的工业级解决方案
- 多级缓存设计&实现;
- 消除限制3: 提供以Map的`keySet`作为Multi的CacheKey参数支持.
- 消除限制4: 自定义`Collection`在注解内开放配置
- 消除限制5: `@CachedPut`注解(调研中)    
- `@Invalid`开启前向清除缓存(调研中)
- 缓存预热(调研中)

---
# 开源依赖
jbox项目室cachex的强依赖之一, 但尚未上传的Maven中央仓库, 有需要编译cachex的同学可自行下载jbox并安装到本地.
项目地址: https://github.com/feiqing/jbox

---
# 合作
cachex项目启动以来, 收到了很多同学的反馈以及协助才能走到今天, 
CacheX是个开放的产品, 如果有希望加入我们的同学可以邮箱联系我, 一起共建cachex的未来!
> jifang.zjf@alibaba-inc.com

----
# 部分使用限制

---
### 1. `@CacheKey`为multi时, 入参list内的元素个数与返回值不一致:
```java
@Cached
public List<FeedUser> invalidMulti(@CacheKey(multi = true, id = "id") List<Long> feedIds) {
   // ...
}
```

> 若以`(1,2,3)`调用`getFeedUsers()`方法, 而实际逻辑执行的确是`{FeedUser(id=1), FeedUser(id=2), FeedUser(id=3), FeedUser(id=4)}`(多一个id=4的对象), 框架只会缓存1、2、3的内容.

### 2. 多个@CacheKey属性为multi
```java
@Cached
public Type multiMulti(@CacheKey(multi = true) List<Long> feedIds, @CacheKey(multi = true) List<Long> authorIds ) {
   // ...
}
```
> 如果多个`@CacheKey`属性为`multi`, 会导致对方法参数做**笛卡尔积计算产生大量的key**, 现阶段的框架还不能支持, 而且我们也不建议这样做(原因是这样很有可能会产生大量无用的缓存).因此这种情况会在代码结构静态扫描时抛出异常, 提示开发者注意.

### 3. 以Map作为multi

```java
@Cached
public Type mapMulti(@CacheKey(multi = true) Map<Long, FeedUser> map) {
   // ...
}
```
> 如果将Map内所有的key-value拼装成key的话, 就会产生类似笛卡尔积的效果, 这种情况我们不建议(同上). 不过如果是将Map.keySet作为拼装Key的列表的功能还是蛮常见的, 如果开发者需要的话我们会在未来对这种情况支持.

### ~~4. 各类怪异的内部容器类调用~~
```java
@Cached
public List<User> invalidCollection(@CacheKey(multi = true, id = "id") List<Long> ids) {
    return Arrays.asList(new User(ids.get(1)), new User(ids.get(2)));
}
```

> 由于multi的Cache在构造容器返回值时需要反射调用容器类的构造方法, 但这些类并未提供公开的构造方法, 因此没法构造出对象, 这类容器有:

- Arrays.ArrayList
- Collections.SingleList
- ...

> @since 1.5.4 该限制不再存在, 详见 1.5.4 release列表.

### 5. 缓存更新
框架现在还只支持添加缓存和失效缓存两种操作, 暂时还不能支持缓存更新(但其实失效后再添加就是更新了O(∩_∩)O~).

> 我们目标在将来提供`@CachePut`注解, 以提供根据方法的入参/返回值进行缓存写入/更新, 详见#TODO列表