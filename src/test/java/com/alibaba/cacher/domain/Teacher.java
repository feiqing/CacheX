package com.alibaba.cacher.domain;

/**
 * @author jifang
 * @since 16/7/22 下午5:57.
 */
public class Teacher {

    private String name;

    public Teacher() {
    }

    public Teacher(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
