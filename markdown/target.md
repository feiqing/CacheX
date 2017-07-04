## 下一里程碑版本目标 2.x
- 缓存防击穿, 参考junnan `poney`实现;
- 多级缓存;
- 限制4: 自定义`Collection`在注解内开放配置
- 限制5:
    - `@CachedPut`注解(开关区分是否返回值, 参考SpringCache的key #result实现)    
- 限制3:
    - 提供以Map的`keySet`作为Multi的CacheKey参数支持.
- 缓存预热(了解ing)
- `@Invalid`是否开启前向清除缓存?