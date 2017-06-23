package com.alibaba.cacher.utils;

import com.alibaba.cacher.Cached;
import com.alibaba.cacher.CachedGet;
import com.alibaba.cacher.Invalid;
import com.alibaba.cacher.enums.Expire;
import com.alibaba.cacher.supplier.ParameterNamesSupplier;
import com.alibaba.cacher.supplier.SpelValueSupplier;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 2017/1/9 下午4:59.
 */
public class CacherSwitcher {

    private static final Map<Method, String[]> methodArgNameMap = new ConcurrentHashMap<>();

    public static boolean isSwitchOn(boolean openStat, Cached cached, Method method, Object[] args) {
        return doIsSwitchOn(openStat, cached.expire(), cached.condition(), method, args);
    }

    public static boolean isSwitchOn(boolean openStat, Invalid invalid, Method method, Object[] args) {
        return doIsSwitchOn(openStat, Expire.FOREVER, invalid.condition(), method, args);
    }

    public static boolean isSwitchOn(boolean openStat, CachedGet cachedGet, Method method, Object[] args) {
        return doIsSwitchOn(openStat, Expire.FOREVER, cachedGet.condition(), method, args);
    }

    private static boolean doIsSwitchOn(boolean openStat,
                                        int expire,
                                        String condition, Method method, Object[] args) {
        if (openStat && expire != Expire.NO) {
            return (boolean) SpelValueSupplier.calcSpelValue(condition,
                    () -> ParameterNamesSupplier.getParameterNames(method),
                    args, true);
        }

        return false;
    }
}
