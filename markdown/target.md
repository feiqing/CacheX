## 1.x TODO
- 缓存防击穿测试
- 性能提升:
    - ~~**DBShooting分组统计队列化**~~
    - ~~**ZKShooting分组统计队列化**~~

----
## 下一里程碑版本目标 2.x
- 架构重构!
- 多级缓存;
- 限制4: 自定义`Collection`在注解内开放配置
- 限制5:
    - `@CachedPut`注解(开关区分是否返回值, 参考SpringCache的key #result实现)    
- 限制3:
    - 提供以Map的`keySet`作为Multi的CacheKey参数支持.
- 缓存预热(了解ing)
- `@Invalid`是否开启前向清除缓存?
- **缓存自动切换**: 设置次数阈值threshold & 时间区间section, 如果在时间区间内失败/超时次数达到阈值, 则自动切换到下一级缓存实现, 如果所有缓存实现均不可用, 则自动将缓存功能关闭.