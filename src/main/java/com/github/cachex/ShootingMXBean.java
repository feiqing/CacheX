package com.github.cachex;

import java.util.Map;

/**
 * @author jifang
 * @since 2017/3/2 上午11:48.
 */
public interface ShootingMXBean {

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

        public static ShootingDO mergeShootingDO(ShootingDO do1, ShootingDO do2) {
            long hit = do1.getHit() + do2.getHit();
            long required = do1.getRequired() + do2.getRequired();

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

    default String summaryName() {
        return "zh".equalsIgnoreCase(System.getProperty("user.language")) ? "全局" : "summary";
    }
}
