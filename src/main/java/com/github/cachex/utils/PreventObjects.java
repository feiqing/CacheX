package com.github.cachex.utils;

import java.io.Serializable;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午5:53.
 */
public class PreventObjects {

    public static Object getPreventObject() {
        return PreventObj.INSTANCE;
    }

    public static boolean isPrevent(Object object) {
        return object == PreventObj.INSTANCE || object instanceof PreventObj;
    }

    private static final class PreventObj implements Serializable {

        private static final long serialVersionUID = -1102811488039755703L;

        private static final PreventObj INSTANCE = new PreventObj();
    }
}
