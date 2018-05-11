package com.github.cachex;

import com.github.cachex.support.annotation.Invalids;

import java.lang.annotation.*;

/**
 * @author jifang
 * @since 16/7/19 下午4:21.
 */
@Documented
@Repeatable(Invalids.class)
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invalid {

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String cache() default "";

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return as {@code @Cached}
     * @since 0.3
     */
    String condition() default "";
}
