package com.alibaba.cacher.domain;

/**
 * @author jifang
 * @since 2016/11/30 上午10:38.
 */
public class Pair<L, R> {

    private L left;

    private R right;

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
