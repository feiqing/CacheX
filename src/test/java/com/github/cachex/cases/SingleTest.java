package com.github.cachex.cases;

import com.github.cachex.cases.base.TestBase;
import com.github.cachex.domain.User;
import com.github.cachex.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author jifang
 * @since 16/7/20 上午10:51.
 */
public class SingleTest extends TestBase {

    @Autowired
    private UserService userService;

    @Test
    public void testSingleKey() throws InterruptedException {
        int id = 1;
        User user = userService.singleKey(id, "ff-no-key", "no-key");
        user = userService.singleKey(id, "ff-no-key", "ls");

        //Thread.sleep(10000000);
    }

    @Test
    public void testRemoveSingleKey() {
        int id = 1;
        String name = "fq";
        User user = userService.singleKey(id, "ff-no-key", "no-key");
        userService.singleRemove(id, name, "not`non");
        user = userService.singleKey(id, "ff-no-key", "ls");
        user = userService.singleKey(id, "ff-no-key", "ls");
    }

    @Test
    public void testSpEL() {
        User user = new User(1, "feiqing", new Date(), 1, "hangz");
        userService.spelCompose(user);
    }
}
