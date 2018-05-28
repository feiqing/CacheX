package com.github.cachex.utils;

import com.github.cachex.Cached;
import com.github.cachex.CachedGet;
import com.github.cachex.Invalid;
import com.github.cachex.core.CacheXConfig;
import com.github.cachex.enums.Expire;
import com.github.cachex.supplier.ParameterNamesSupplier;
import com.github.cachex.supplier.SpelValueSupplier;

import java.lang.reflect.Method;

/**
 * @author jifang
 * @since 2017/1/9 下午4:59.
 */
public class SwitcherUtils {

    public static boolean isSwitchOn(CacheXConfig config, Cached cached, Method method, Object[] args) {
        return isSwitchOn(config.getCachex() == CacheXConfig.Switch.ON, cached, method, args);
    }

    public static boolean isSwitchOn(boolean openStat, Cached cached, Method method, Object[] args) {
        return doIsSwitchOn(openStat, cached.expire(), cached.condition(), method, args);
    }

    public static boolean isSwitchOn(CacheXConfig config, Invalid invalid, Method method, Object[] args) {
        return doIsSwitchOn(config.getCachex() == CacheXConfig.Switch.ON, Expire.FOREVER, invalid.condition(), method, args);
    }

    public static boolean isSwitchOn(CacheXConfig config, CachedGet cachedGet, Method method, Object[] args) {
        return doIsSwitchOn(config.getCachex() == CacheXConfig.Switch.ON, Expire.FOREVER, cachedGet.condition(), method, args);
    }

    private static boolean doIsSwitchOn(boolean openStat,
                                        int expire,
                                        String condition, Method method, Object[] args) {
        return openStat
                && expire != Expire.NO
                && (boolean) SpelValueSupplier.calcSpelValue(condition, () -> ParameterNamesSupplier.getParameterNames(method), args, true);

    }
}
