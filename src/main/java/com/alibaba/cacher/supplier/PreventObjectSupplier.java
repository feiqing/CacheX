package com.alibaba.cacher.supplier;

import java.io.Serializable;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午5:53.
 */
public class PreventObjectSupplier {

    public static Object generatePreventObject() {
        return PreventObject.INSTANCE;
    }

    public static boolean isGeneratePreventObject(Object object) {
        return object == PreventObject.INSTANCE || object instanceof PreventObject;
    }

    private static final class PreventObject implements Serializable {

        private static final long serialVersionUID = -1102811488039755703L;

        private static final PreventObject INSTANCE = new PreventObject();
    }
}
