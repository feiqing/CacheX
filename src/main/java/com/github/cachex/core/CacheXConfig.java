package com.github.cachex.core;

import com.github.cachex.ICache;
import com.github.cachex.ShootingMXBean;
import lombok.Data;

import java.util.Map;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午3:46.
 */
@Data
public class CacheXConfig {

    // ICache接口实现
    private Map<String, ICache> caches;

    // 缓存分组命中率统计
    private ShootingMXBean shootingMXBean;

    // 是否开启CacheX(全局开关)
    private Switch cachex;

    // 是否开启缓存防击穿
    private Switch prevent;

    public boolean isPreventOn() {
        return prevent != null && prevent == Switch.ON;
    }

    public static CacheXConfig newConfig(Map<String, ICache> caches) {
        CacheXConfig config = new CacheXConfig();
        config.caches = caches;
        config.cachex = Switch.ON;
        config.prevent = Switch.OFF;
        config.shootingMXBean = null;

        return config;
    }

    public enum Switch {
        ON,
        OFF
    }
}
