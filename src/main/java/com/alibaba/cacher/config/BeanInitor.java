package com.alibaba.cacher.config;

import java.lang.management.ManagementFactory;

/**
 * @author jifang
 * @since 2017/4/6 下午3:31.
 */
public class BeanInitor {

    public static Object mBeanServer(){
        return ManagementFactory.getPlatformMBeanServer();
    }
}
