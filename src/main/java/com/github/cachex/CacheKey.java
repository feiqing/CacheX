package com.github.cachex;

import java.lang.annotation.*;

/**
 * @author jifang
 * @since 16/7/19 下午6:07.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheKey {

    /**
     * @return use a part of param as a cache key part
     */
    String value() default "";

    /**
     * @return used multi model(value has `#i` index) and method return {@code Collection},
     * the {@code field} indicate which of the {@code Collection}'s entity field related with this param
     */
    String field() default "";
}
