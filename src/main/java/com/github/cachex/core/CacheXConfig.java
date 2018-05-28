package com.github.cachex.core;

import com.github.cachex.ICache;
import com.github.cachex.ShootingMXBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午3:46.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheXConfig {

    // ICache接口实现
    private Map<String, ICache> caches;

    // 缓存分组命中率统计
    private ShootingMXBean shootingMXBean;

    // 是否开启CacheX(全局开关)
    private Switch cachex;

    // 是否开启缓存防击穿
    private Switch protect;

    public CacheXConfig(Map<String, ICache> caches) {
        this.caches = caches;
        this.cachex = Switch.ON;
        this.protect = Switch.OFF;
        this.shootingMXBean = null;
    }

    public enum Switch {
        ON,
        OFF
    }
}
