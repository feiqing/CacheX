package com.github.cachex.domain;

import java.util.*;

/**
 * @author jifang
 * @since 2016/11/2 下午5:45.
 */
public class CacheReadResult {

    private Map<String, Object> hitKeyMap;

    private Set<String> missKeySet;

    public CacheReadResult() {
    }

    public CacheReadResult(Map<String, Object> hitKeyMap, Set<String> missKeySet) {
        this.hitKeyMap = hitKeyMap;
        this.missKeySet = missKeySet;
    }

    public Map<String, Object> getHitKeyMap() {
        return hitKeyMap == null ? Collections.emptyMap() : hitKeyMap;
    }

    public Set<String> getMissKeySet() {
        return missKeySet == null ? Collections.emptySet() : missKeySet;
    }
}
