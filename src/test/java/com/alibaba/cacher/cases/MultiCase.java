package com.alibaba.cacher.cases;

import com.alibaba.cacher.Utils;
import com.alibaba.cacher.cases.base.TestBase;
import com.alibaba.cacher.domain.User;
import com.alibaba.cacher.service.UserService;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jifang
 * @since 2016/11/30 下午2:19.
 */
public class MultiCase extends TestBase {

    @Autowired
    private UserService service;

    @Test
    public void testReturnMap() throws InterruptedException {
        List<Integer> ids = new LinkedList<>();
        for (int i = 0; i < 20; ++i) {
            ids.add(i);
        }
        Map<Integer, User> map = service.returnMap("name", ids, "ok");
        map = service.returnMap("name", ids, "ok");

        ids.add(new Random().nextInt());
        map = service.returnMap("name", ids, "ok");
    }

    @Test
    public void testRandomGet() {
        for (int i = 0; i < 1000; ++i) {
            List<Integer> ids = Stream.generate(this::nextRandom).limit(nextLitterRandom()).collect(Collectors.toList());
            service.returnList(ids, "name", new Object());
            service.returnMap("app", ids, new Object());

            Utils.delay(10000);
        }
    }

    private int nextRandom() {
        return (int) (Math.random() * 1000);
    }

    private int nextLitterRandom() {
        return (int) (Math.random() * 100);
    }

    @Test
    public void multiInvalidate() {
        List<Integer> ids = new LinkedList<>();
        for (int i = 0; i < 2; ++i) {
            ids.add(i);
        }
        service.multiInvalid("name", ids);
    }

    @Test
    public void testReturnList() {
        List<Integer> ids = Lists.newArrayList(1, 2, 3, 4);
        service.returnList(ids, "ss", new Object());
        service.returnList(ids, "ss", new Object());

        ids.add(new Random().nextInt());
        service.returnList(ids, "ss", new Object());
    }

    @Test
    public void testUpdateList() {
        List<User> users = new ArrayList<>();
        users.add(new User(1, null, null, 1, null));
        users.add(new User(2, null, null, 1, null));
        service.batchUpdateList(users);
    }
}
