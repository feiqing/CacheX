package com.alibaba.cacher.config;

import java.lang.annotation.*;

/**
 * @author jifang
 * @since 2017/3/16 下午5:19.
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    String qualifierName() default "";

    Class<?> qualifierClass() default Object.class;
}
