package com.alibaba.cacher.hitrate;

import java.util.Map;

/**
 * @author jifang
 * @since 2017/3/2 上午11:48.
 */
public interface HitRateMXBean {

    default String getSummaryName() {
        return "zh".equalsIgnoreCase(System.getProperty("user.language")) ? "全局命中率" : "summary";
    }

    void hitIncr(String pattern, int count);

    void requireIncr(String pattern, int count);

    Map<String, RateDO> getHitRate();

    void reset(String pattern);

    void resetAll();

    class RateDO {
        private long hit;

        private long required;

        private String rate;

        public static RateDO newInstance(long hit, long required) {
            double rate = (required == 0 ? 0.0 : hit * 100.0 / required);
            String rateStr = String.format("%.1f%s", rate, "%");

            return new RateDO(hit, required, rateStr);
        }

        private RateDO(long hit, long required, String rate) {
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
