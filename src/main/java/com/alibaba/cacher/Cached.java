package com.alibaba.cacher;

import com.alibaba.cacher.enums.Expire;
import com.alibaba.cacher.support.annotation.Cacheds;

import java.lang.annotation.*;

/**
 * @author jifang
 * @since 2016/11/2 下午2:22.
 */
@Documented
@Repeatable(Cacheds.class)
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {

    /**
     * @return Specifies the <b>Used cache implementation</b>,
     * default the first {@code caches} config in {@code CacherAspect}
     * @since 0.3
     */
    String cache() default "";

    /**
     * @return Specifies the start prefix on every key,
     * if the {@code Method} have non {@code param},
     * {@code prefix} is the <b>constant key</b> used by this {@code Method}
     * @since 0.3
     */
    String prefix() default "";

    /**
     * @return use <b>SpEL</b>,
     * when this spel is {@code true}, this {@Code Method} will go through by cache
     * @since 0.3
     */
    String condition() default "";

    /**
     * @return expire time, time unit: <b>seconds</b>
     */
    int expire() default Expire.FOREVER;
}