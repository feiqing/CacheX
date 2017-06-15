package com.alibaba.cacher.support.annotation;

import com.alibaba.cacher.Cached;

import java.lang.annotation.*;

/**
 * @author jifang.zjf
 * @since 2017/6/14 下午11:22.
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheds {
    Cached[] value();
}
