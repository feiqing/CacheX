package com.github.cachex;

import java.util.Random;

/**
 * @author jifang.zjf
 * @since 2017/6/14 下午9:14.
 */
public class Utils {

    public static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int nextRadom() {
        return new Random().nextInt();
    }
}
