package com.alibaba.cacher.support.annotation;

import com.alibaba.cacher.CachedGet;

import java.lang.annotation.*;

/**
 * @author jifang.zjf
 * @since 2017/6/22 下午2:02.
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedGets {
    CachedGet[] value();
}

