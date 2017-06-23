package com.alibaba.cacher.ioc;

import java.lang.annotation.*;

/**
 * @author jifang
 * @since 2017/3/16 下午5:18.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Singleton {

    boolean spring() default false;

    String name() default "";
}