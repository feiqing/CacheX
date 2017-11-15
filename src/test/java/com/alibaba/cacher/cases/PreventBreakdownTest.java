package com.alibaba.cacher.cases;

import com.alibaba.cacher.cases.base.TestBase;
import com.alibaba.cacher.domain.User;
import com.alibaba.cacher.service.impl.PreventBreakdownServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author jifang.zjf
 * @since 2017/7/6 上午11:12.
 */
public class PreventBreakdownTest extends TestBase {


    @Autowired
    private PreventBreakdownServiceImpl service;

    @Test
    public void test() throws InterruptedException {
        List<Integer> ids = IntStream.range(0, 3).boxed().collect(Collectors.toList());

        Map<Integer, User> map = service.getMap(ids);
        System.out.println("first map.size() = " + map.size());

        ids.add(4);
        map = service.getMap(ids);
        System.out.println("second map.size() = " + map.size());


        Thread.sleep(100000);
    }
}
