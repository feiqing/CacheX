## 限制

---
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