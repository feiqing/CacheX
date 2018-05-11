package com.github.cachex.core;

import com.github.cachex.ShootingMXBean;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午3:46.
 */
public class Config {

    // cacher全局开关
    private volatile boolean open = true;

    // 开启缓存防击穿功能
    private volatile boolean preventBreakdown = false;

    // 缓存分组命中率统计
    private ShootingMXBean shootingMXBean;

    public Config(boolean open, boolean preventBreakdown, ShootingMXBean shootingMXBean) {
        this.open = open;
        this.preventBreakdown = preventBreakdown;
        this.shootingMXBean = shootingMXBean;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isPreventBreakdown() {
        return preventBreakdown;
    }

    public void setPreventBreakdown(boolean preventBreakdown) {
        this.preventBreakdown = preventBreakdown;
    }

    public ShootingMXBean getShootingMXBean() {
        return shootingMXBean;
    }

    public void setShootingMXBean(ShootingMXBean shootingMXBean) {
        this.shootingMXBean = shootingMXBean;
    }
}
