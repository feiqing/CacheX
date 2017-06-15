package com.alibaba.cacher.annotation;

/**
 * @author jifang.zjf
 * @since 2017/6/15 上午10:13.
 */
@InheritedTest("使用Inherited的注解 class")
@NoInheritedTest("未使用Inherited的注解 class")
public class Parent {

    @InheritedTest("使用Inherited的注解 method")
    @NoInheritedTest("未使用Inherited的注解 method")
    public void method(){

    }
    @InheritedTest("使用Inherited的注解 method2")
    @NoInheritedTest("未使用Inherited的注解 method2")
    public void method2(){

    }

    @InheritedTest("使用Inherited的注解 field")
    @NoInheritedTest("未使用Inherited的注解 field")
    public String a;
}