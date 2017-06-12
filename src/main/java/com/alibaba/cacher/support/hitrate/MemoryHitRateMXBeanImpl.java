package com.alibaba.cacher.support.hitrate;

import com.alibaba.cacher.hitrate.HitRateMXBean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jifang
 * @since 2017/3/2 下午2:28.
 */
public class MemoryHitRateMXBeanImpl implements HitRateMXBean {

    private ConcurrentMap<String, AtomicLong> hitMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, AtomicLong> requireMap = new ConcurrentHashMap<>();

    @Override
    public void hitIncr(String pattern, int count) {
        hitMap.computeIfAbsent(
                pattern,
                (k) -> new AtomicLong()
        ).addAndGet(count);
    }

    @Override
    public void requireIncr(String pattern, int count) {
        requireMap.computeIfAbsent(
                pattern,
                (k) -> new AtomicLong()
        ).addAndGet(count);
    }

    @Override
    public Map<String, RateDO> getHitRate() {
        Map<String, RateDO> result = new LinkedHashMap<>();

        AtomicLong totalHit = new AtomicLong(0);
        AtomicLong totalRequire = new AtomicLong(0);
        requireMap.forEach((pattern, count) -> {
            long hit = hitMap.getOrDefault(pattern, new AtomicLong(0)).get();
            long require = count.get();

            totalHit.addAndGet(hit);
            totalRequire.addAndGet(require);

            result.put(pattern, RateDO.newInstance(hit, require));
        });

        // 全局命中率
        result.put(getSummaryName(), RateDO.newInstance(totalHit.get(), totalRequire.get()));

        return result;
    }

    @Override
    public void reset(String pattern) {
        hitMap.remove(pattern);
        requireMap.remove(pattern);
    }

    @Override
    public void resetAll() {
        hitMap.clear();
        requireMap.clear();
    }
}
