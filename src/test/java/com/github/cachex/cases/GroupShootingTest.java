package com.github.cachex.cases;

import com.github.cachex.cases.base.TestBase;
import com.github.cachex.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author jifang.zjf
 * @since 2017/7/6 下午2:48.
 */
public class GroupShootingTest extends TestBase {

    @Autowired
    private UserService userService;

    @Test
    public void test() {
        System.out.println(Integer.MAX_VALUE);
        List<Integer> ids = IntStream.range(0, 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        userService.getUsers(ids, "name", "no");
        userService.getUsers(ids, "name", "no");
    }
}
