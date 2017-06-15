package com.alibaba.cacher.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author jifang.zjf
 * @since 2017/6/15 上午10:20.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface InheritedTest {
    String value();
}