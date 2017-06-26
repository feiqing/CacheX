package com.alibaba.cacher.domain;

import java.util.*;

/**
 * @author jifang
 * @since 2016/11/2 下午5:45.
 */
public class BatchReadResult {

    private Map<String, Object> hitKeyValueMap;

    private Set<String> missKeys;

    public BatchReadResult() {
    }

    public BatchReadResult(Map<String, Object> hitKeyValueMap, Set<String> missKeys) {
        this.hitKeyValueMap = hitKeyValueMap;
        this.missKeys = missKeys;
    }

    public Map<String, Object> getHitKeyValueMap() {
        return hitKeyValueMap == null ? Collections.emptyMap() : hitKeyValueMap;
    }

    public Set<String> getMissKeys() {
        return missKeys == null ? Collections.emptySet() : missKeys;
    }
}
