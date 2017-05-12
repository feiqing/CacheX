package com.alibaba.cacher.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 2017/3/16 下午5:21.
 */
@SuppressWarnings("unchecked")
public class CacherContainer {

    private static final Map<String, Object> container = new ConcurrentHashMap<>();

    public static void init(Object root) {


        /**
         * 1. package scan
         * 2. init @Single class newInstance()
         * 3. put container
         *      3.1 init config class(从配置文件中制定的class, 而非@Single标定的, 如jmxSoppert)
         *      3.2 put container
         *
         *
         * 4. for each `container`
         * 5. inject Object
         */
    }
}
