package com.alibaba.cacher.hitrate;

import com.alibaba.cacher.support.hitrate.DerbyHitRateMXBeanImpl;
import com.alibaba.cacher.support.hitrate.H2HitRateMXBeanImpl;
import org.junit.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * @author jifang.zjf
 * @since 2017/6/10 下午1:32.
 */
public class MxBeanTest {

    @Test
    public void test() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, InterruptedException {
        HitRateMXBean mxBean = new DerbyHitRateMXBeanImpl();

        ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, new ObjectName("com.alibaba.cacher:name=hit"));
        mxBean.hitIncr("nihao", 1);
        mxBean.requireIncr("nihao", 2);

        Thread.sleep(1000000);
        //mxBean.reset("nihao");

        //mxBean.requireIncr("test", 88);
        //mxBean.resetAll();
    }

    @Test
    public void testH2() throws InterruptedException, MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        HitRateMXBean mxBean = new H2HitRateMXBeanImpl();

        ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, new ObjectName("com.alibaba.cacher:name=hit"));
        mxBean.hitIncr("nihao", 1);
        mxBean.requireIncr("nihao", 2);

        Thread.sleep(1000000);
    }
}
