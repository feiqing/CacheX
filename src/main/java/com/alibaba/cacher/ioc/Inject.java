package com.alibaba.cacher.ioc;

import java.lang.annotation.*;

/**
 * @author jifang
 * @since 2017/3/16 下午5:19.
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    boolean optional() default false;

    String qualifierName() default "";

    Class<?> qualifierClass() default Object.class;
}
