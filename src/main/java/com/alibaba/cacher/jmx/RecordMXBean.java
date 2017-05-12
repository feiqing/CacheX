package com.alibaba.cacher.jmx;

import com.alibaba.cacher.utils.CacherUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jifang
 * @since 2017/3/2 上午11:48.
 */
public interface RecordMXBean {

    Map<String, AtomicLong> HIT_COUNT_MAP =
            CacherUtils.createAtomicMapProxy(new ConcurrentHashMap<String, AtomicLong>());

    Map<String, AtomicLong> REQUIRE_COUNT_MAP =
            CacherUtils.createAtomicMapProxy(new ConcurrentHashMap<String, AtomicLong>());

    Map<String, Rate> getHitRate();

    void resetPatternRate(String pattern);

    void resetAllRate();

    class Rate {
        private long hit;

        private long required;

        private String rate;

        public Rate(long hit, long required, String rate) {
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
