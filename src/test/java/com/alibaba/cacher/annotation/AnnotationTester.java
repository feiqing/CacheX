package com.alibaba.cacher.annotation;

import com.alibaba.cacher.Cached;
import com.alibaba.cacher.service.impl.UserServiceImpl;
import com.alibaba.cacher.support.annotation.Cacheds;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author jifang.zjf
 * @since 2017/6/14 下午11:26.
 */
public class AnnotationTester {

    @Test
    public void test1() throws NoSuchMethodException {
        UserServiceImpl userService = new UserServiceImpl();
        Method returnMap = userService.getClass().getDeclaredMethod("returnMap", String.class, List.class, Object.class);
        System.out.println(Arrays.toString(returnMap.getDeclaredAnnotations()));

        Cacheds declaredAnnotation = returnMap.getDeclaredAnnotation(Cacheds.class);
        System.out.println(declaredAnnotation);

        Cached[] declaredAnnotationsByType = returnMap.getDeclaredAnnotationsByType(Cached.class);
        System.out.println(declaredAnnotation);
    }

    @Test
    public void testInherited() throws NoSuchMethodException, NoSuchFieldException {
        Class<Child> clazz = Child.class;
        //对类进行测试
        System.out.println("对类进行测试");
        if (clazz.isAnnotationPresent(InheritedTest.class)) {
            System.out.println(clazz.getAnnotation(InheritedTest.class).value());
        }
        if (clazz.isAnnotationPresent(NoInheritedTest.class)) {
            System.out.println(clazz.getAnnotation(NoInheritedTest.class).value());
        }
        System.out.println();
        //对方法 进行测试
        System.out.println("对方法进行测试");
        Method method = clazz.getMethod("method", null);
        if (method.isAnnotationPresent(InheritedTest.class)) {
            System.out.println(method.getAnnotation(InheritedTest.class).value());
        }
        if (method.isAnnotationPresent(NoInheritedTest.class)) {
            System.out.println(method.getAnnotation(NoInheritedTest.class).value());
        }
        System.out.println();
        //对方法2 进行测试
        System.out.println("对方法2进行测试");
        Method method2 = clazz.getMethod("method2", null);
        if (method2.isAnnotationPresent(InheritedTest.class)) {
            System.out.println(method2.getAnnotation(InheritedTest.class).value());
        }
        if (method2.isAnnotationPresent(NoInheritedTest.class)) {
            System.out.println(method2.getAnnotation(NoInheritedTest.class).value());
        }
        System.out.println();
        //对属性测试
        System.out.println("对属性进行测试");
        Field field = clazz.getField("a");
        if (field.isAnnotationPresent(InheritedTest.class)) {
            System.out.println(field.getAnnotation(InheritedTest.class).value());
        }
        if (field.isAnnotationPresent(NoInheritedTest.class)) {
            System.out.println(field.getAnnotation(NoInheritedTest.class).value());
        }
    }
}
