package com.github.cachex.support.annotation;

import com.github.cachex.Invalid;

import java.lang.annotation.*;

/**
 * @author jifang.zjf
 * @since 2017/6/14 下午11:22.
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invalids {
    Invalid[] value();
}
