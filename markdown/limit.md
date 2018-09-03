# TODO: 目标: 工业级缓存解决方案
- 多级缓存设计&实现(调研中);
- 消除限制5: `@CachedPut`注解(调研中);
- `@Invalid`开启前向清除缓存(调研中);
- 缓存预热(调研中);

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
# CacheX使用限制

---
### 1. 多个@CacheKey属性为批量模式
```
@Cached
Object func(@CacheKey("#arg0[#i]") List<Long> fIds, @CacheKey("#arg1[#i]") List<Long> aIds);
```
> 该模式会导致CacheX对方法参数做**笛卡尔积**, 结果将会计算产生大量的缓存`key`, 性能损耗较大, 因此不支持.

### 2. 以参数Map作为批量模式参数

```
@Cached
Object func(@CacheKey("#arg0[#i]") Map<Long, Object> map);
```
> 同上: 如果将Map内所有的`Key`/`Value`进行交叉拼装为缓存`key`的话, 也会产生类似**笛卡尔积**的效果, 因此也不支持.

### 3. 以Map.keySet()作为批量模式参数
```
@Cached
Object func(@CacheKey("#arg0.keySet()[#i]") Map<Long, Object> map);
```
> 这种模式不常用且实现复杂、性能损耗较大, 因此不支持.

### 4. 非标准容器作为批量模式参数
```
@Cached
Object func(@CacheKey("#arg0[#i]") List<Long> ids) {
    return Collections.emptyList();
}
```

> 由于在批量模式下, CacheX会在构造容器返回值时反射调用容器类的默认构造方法, 以及向容器内添加元素, 但这些容器并未暴露这些方法, 因此不能支持.

- 这类容器有:
    - Arrays.ArrayList
    - Collections.SingleList
    - ...
> 老版本的CacheX曾经支持过一段时间, 但由于在方法返回前需要转换为MutableCollection, 在方法返回时又要转换回去, 性能损耗较大, 因此后面就废掉了.

### 5. 缓存更新
框架现在还只支持添加缓存和失效缓存两种操作, 暂时还不能支持缓存更新(但其实失效后再添加就是更新了O(∩_∩)O~).

> 我们目标在将来提供`@CachePut`注解, 以提供根据方法的入参/返回值进行缓存写入/更新, 详见#TODO列表