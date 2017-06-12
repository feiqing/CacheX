package com.alibaba.cacher.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jifang.
 * @since 2016/5/26 11:16.
 */
public class User implements Serializable {

    private static final long serialVersionUID = -1843332169074089984L;

    private int id;

    private String name;

    private Date birthday;

    private int sex;

    private String address;

    private Teacher teacher;

    public User() {
    }

    public User(int id, String name, Date birthday, int sex, String address) {
        this.id = id;
        this.name = name;
        this.birthday = birthday;
        this.sex = sex;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
}