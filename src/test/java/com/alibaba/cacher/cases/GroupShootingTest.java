package com.alibaba.cacher.cases;

import com.alibaba.cacher.cases.base.TestBase;
import com.alibaba.cacher.service.UserService;
import com.alibaba.cacher.service.impl.InnerMapService;
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

    @Autowired
    private InnerMapService mapService;

    @Test
    public void test() throws InterruptedException {
        System.out.println(Integer.MAX_VALUE);
        List<Integer> ids = IntStream.range(0, 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        userService.returnList(ids, "name", "no");
        userService.returnList(ids, "name", "no");
        mapService.immutableMap(ids);
        mapService.immutableMap(ids);

        Thread.sleep(1000000);
    }
}
