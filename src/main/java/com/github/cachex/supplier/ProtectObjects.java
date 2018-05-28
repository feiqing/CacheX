package com.github.cachex.supplier;

import java.io.Serializable;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午5:53.
 */
public class ProtectObjects {

    public static Object getProtectObject() {
        return ProtectObject.INSTANCE;
    }

    public static boolean isProtect(Object object) {
        return object == ProtectObject.INSTANCE || object instanceof ProtectObject;
    }

    private static final class ProtectObject implements Serializable {

        private static final long serialVersionUID = -1102811488039755703L;

        private static final ProtectObject INSTANCE = new ProtectObject();
    }
}
