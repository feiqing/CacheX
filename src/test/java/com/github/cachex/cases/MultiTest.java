package com.github.cachex.cases;

import com.github.cachex.Utils;
import com.github.cachex.cases.base.TestBase;
import com.github.cachex.domain.User;
import com.github.cachex.service.UserService;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author jifang
 * @since 2016/11/30 下午2:19.
 */
public class MultiTest extends TestBase {

    @Autowired
    private UserService service;

    @Test
    public void testReturnList() {
        System.out.println("#########---");
        List<Integer> ids = Lists.newArrayList(1, 2, 3, 4);
        service.getUsers(ids, "ss", new Object());
        System.out.println("---");
        service.getUsers(ids, "ss", new Object());
        System.out.println("---");
        ids.add(new Random().nextInt());
        List<User> users = service.getUsers(ids, "ss", new Object());
        System.out.println("---");
        service.invalidList(users);
        System.out.println("---");
        service.getUsers(ids, "ss", new Object());
        System.out.println("#########---");
    }

    @Test
    public void testReturnMap() {
        System.out.println("#########---");
        List<Integer> ids = Lists.newArrayList(0, 1, 2);
        service.returnMap("name", ids, "ok");
        System.out.println("---");
        service.returnMap("name", ids, "ok");
        System.out.println("---");
        ids.add(new Random().nextInt());
        service.returnMap("name", ids, "ok");
        System.out.println("---");
        service.invalidMap("name", ids);
        System.out.println("---");
        service.returnMap("name", ids, "ok");
        System.out.println("---");
        service.returnMap("name", ids, "ok");
        System.out.println("#########---");
    }

    @Test
    public void testRandomGet() {


        for (int i = 0; i < 50; ++i) {
//            List<Integer> ids = Stream.generate(this::nextRandom).limit(nextLitterRandom()).collect(Collectors.toList());
            List<Integer> collect = Stream.generate(this::nextRandom).limit(100).collect(toList());
            service.getUsers(collect, "name", new Object());
            // service.returnMap("app", ids, new Object());

            Utils.delay(100);
        }
    }

    private int nextRandom() {
        return (int) (Math.random() * 100);
    }

    private int nextLitterRandom() {
        return (int) (Math.random() * 100);
    }
}
