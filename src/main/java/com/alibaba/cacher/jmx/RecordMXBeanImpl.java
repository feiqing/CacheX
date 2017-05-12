package com.alibaba.cacher.jmx;

import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.constant.Constant;
import org.apache.commons.lang3.StringUtils;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jifang
 * @since 2017/3/2 下午2:28.
 */
@Singleton
public class RecordMXBeanImpl extends NotificationBroadcasterSupport implements RecordMXBean {

    private long seq = 0;

    @Override
    public Map<String, Rate> getHitRate() {
        Map<String, Rate> result = new LinkedHashMap<>();
        Map<String, Rate> subResult = new HashMap<>();
        for (Map.Entry<String, AtomicLong> requireEntry : REQUIRE_COUNT_MAP.entrySet()) {
            String keyPattern = requireEntry.getKey();
            if (!StringUtils.equals(keyPattern, Constant.TOTAL_KEY)) {
                subResult.put(keyPattern, toRate(requireEntry));
            } else {
                result.put(keyPattern, toRate(requireEntry));
            }
        }

        result.putAll(subResult);

        return result;
    }

    @Override
    public void resetPatternRate(String pattern) {
        if (REQUIRE_COUNT_MAP.containsKey(pattern)) {
            long hitCount = HIT_COUNT_MAP.get(pattern).getAndSet(0);
            long requireCount = REQUIRE_COUNT_MAP.get(pattern).getAndSet(0);

            HIT_COUNT_MAP.get(Constant.TOTAL_KEY).addAndGet(hitCount * -1);
            REQUIRE_COUNT_MAP.get(Constant.TOTAL_KEY).addAndGet(requireCount * -1);
        } else {
            String msg = String.format("please check the pattern [%s] is not correct, useful patterns %s",
                    pattern,
                    REQUIRE_COUNT_MAP.keySet());
            Notification notification = new Notification("error", this, seq++, msg);
            sendNotification(notification);
        }
    }

    @Override
    public void resetAllRate() {
        for (Map.Entry<String, AtomicLong> entry : REQUIRE_COUNT_MAP.entrySet()) {
            entry.getValue().set(0);
            HIT_COUNT_MAP.get(entry.getKey()).set(0);
        }
    }

    private Rate toRate(Map.Entry<String, AtomicLong> requireEntry) {
        long hitCount = HIT_COUNT_MAP.get(requireEntry.getKey()).get();
        long requireCount = requireEntry.getValue().get();
        double rate = (requireCount == 0 ? 0.0 : hitCount * 100.0 / requireCount);
        String rateStr = String.format("%.1f%s", rate, "%");

        return new Rate(hitCount, requireCount, rateStr);
    }
}
