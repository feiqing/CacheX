package com.alibaba.cacher.shooting;

import java.util.Map;

/**
 * @author jifang
 * @since 2017/3/2 上午11:48.
 */
public interface ShootingMXBean {

    default String getSummaryName() {
        return "zh".equalsIgnoreCase(System.getProperty("user.language")) ? "全局命中率" : "summary shooting";
    }

    void hitIncr(String pattern, int count);

    void requireIncr(String pattern, int count);

    Map<String, ShootingDO> getShooting();

    void reset(String pattern);

    void resetAll();

    class ShootingDO {

        private long hit;

        private long required;

        private String rate;

        public static ShootingDO newInstance(long hit, long required) {
            double rate = (required == 0 ? 0.0 : hit * 100.0 / required);
            String rateStr = String.format("%.1f%s", rate, "%");

            return new ShootingDO(hit, required, rateStr);
        }

        /**
         * 对两个相同pattern的ShootingDO进行合并:
         * 由于Derby与H2建表时都对pattern字段进行了unique,
         * 因此在此处不会出现key冲突的情况, 因此永远都不会调到该function
         */
        public static ShootingDO mergeShootingDO(ShootingDO shootingDO1, ShootingDO shootingDO2) {
            long hit = shootingDO1.getHit() + shootingDO2.getHit();
            long required = shootingDO1.getRequired() + shootingDO2.getRequired();

            return newInstance(hit, required);
        }

        private ShootingDO(long hit, long required, String rate) {
            this.hit = hit;
            this.required = required;
            this.rate = rate;
        }

        public long getHit() {
            return hit;
        }

        public long getRequired() {
            return required;
        }

        public String getRate() {
            return rate;
        }
    }
}
