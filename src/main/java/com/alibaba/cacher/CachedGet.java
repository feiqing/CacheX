package com.alibaba.cacher;

import com.alibaba.cacher.support.annotation.CachedGets;

import java.lang.annotation.*;

/**
 * @author jifang.zjf
 * @since 2017/6/22 下午2:02.
 */
@Documented
@Repeatable(CachedGets.class)
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedGet {

    /**
     * @return Specifies the <b>Used cache implementation</b>,
     * default the first {@code caches} config in {@code CacherAspect}
     * @since 0.3
     */
    String cache() default "";

    /**
     * @return Specifies the start keyExp on every key,
     * if the {@code Method} have non {@code param},
     * {@code keyExp} is the <b>constant key</b> used by this {@code Method}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return use <b>SpEL</b>,
     * when this spel is {@code true}, this {@Code Method} will go through by cache
     * @since 0.3
     */
    String condition() default "";
}
