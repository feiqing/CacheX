package com.github.cachex;

import com.github.cachex.domain.User;
import com.github.cachex.supplier.SpelValueSupplier;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author jifang.zjf
 * @since 2017/6/23 上午11:15.
 */
public class ObjectTestCase {

    @Test
    public void test() {
        List<User> users = Arrays.asList(new User(1, "fq1"), new User(2, "fq2"));
        String value = (String) SpelValueSupplier.calcSpelValue("${#users[#index].name}feiqing", new String[]{"users", "user", "index"}, new Object[]{users, new User(3, "fq3"), 0}, "kong");
        System.out.println(value);
    }
}
