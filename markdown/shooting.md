## 命中率分组统计

### 配置实例
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop.xsd">

    <!-- 启用自动代理: 如果已经开启则不必重复开启 -->
    <aop:aspectj-autoproxy/>

    <!-- 注入Cacher切面:
            open: 定义Cacher的全局开关
            caches: 只要实现了ICache接口的cache产品均可被Cacher托管
     -->
    <bean class="com.alibaba.cacher.CacherAspect">
        <constructor-arg name="open" value="true"/>
        <constructor-arg name="caches">
            <map key-returnType="java.lang.String" value-returnType="com.alibaba.cacher.ICache">
                <entry key="tair" value-ref="tair"/>
            </map>
        </constructor-arg>
        <!-- shootingMXBean 开启缓存命中率分组统计功能, 注入基于zk的实现-->
        <constructor-arg name="shootingMXBean" ref="zkShootingMXBean"/>
    </bean>

    <!-- 命中率统计: 根据具体业务场景选择一款实现注入即可 -->
    <bean id="memoryShootingMXBean" class="com.alibaba.cacher.support.shooting.MemoryShootingMXBeanImpl"
          lazy-init="true"/>
    <bean id="derbyShootingMXBean" class="com.alibaba.cacher.support.shooting.DerbyShootingMXBeanImpl"
          lazy-init="true"/>
    <bean id="h2ShootingMXBean" class="com.alibaba.cacher.support.shooting.H2ShootingMXBeanImpl"
          lazy-init="true"/>
    <bean id="zkShootingMXBean" class="com.alibaba.cacher.support.shooting.ZKShootingMXBeanImpl"
          destroy-method="tearDown"
          lazy-init="true">
        <constructor-arg name="productName" value="cacher-tester"/>
        <constructor-arg name="zkServers" value="139.129.9.166:2181"/>
    </bean>
    
    <bean id="tair" class="com.alibaba.cacher.support.cache.TairCache" lazy-init="true">
        <constructor-arg name="configId" value="mdbcomm-daily"/>
        <constructor-arg name="namespace" value="180"/>
    </bean>

</beans>
```
### 其他待续...