package com.alibaba.cacher.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author jifang.zjf
 * @since 2017/6/15 上午10:21.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NoInheritedTest {
    String value();
}
