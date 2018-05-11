## 1.6 TODO
- 测试通过
- 缓存防击穿测试
- 去掉自己实现的di, 引入google guice(不能注入的选用Setter方法)
- 目标成为一个可以工业级的解决方案

----
## 下一里程碑版本目标 2.x
- 架构重构!
- 限制4: 自定义`Collection`在注解内开放配置
- 限制5:
    - `@CachedPut`注解(开关区分是否返回值, 参考SpringCache的key #result实现)    
- 限制3:
    - 提供以Map的`keySet`作为Multi的CacheKey参数支持.
- `@Invalid`是否开启前向清除缓存