package com.github.cachex.shooting;

import com.github.cachex.ShootingMXBean;
import com.github.cachex.Utils;
import com.github.cachex.support.shooting.DerbyShootingMXBeanImpl;
import com.github.cachex.support.shooting.H2ShootingMXBeanImpl;
import com.github.cachex.support.shooting.ZKShootingMXBeanImpl;
import org.junit.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * @author jifang.zjf
 * @since 2017/6/10 下午1:32.
 */
public class MxBeanTest {

    @Test
    public void testDerby() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, InterruptedException {
        ShootingMXBean mxBean = new DerbyShootingMXBeanImpl();

        ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, new ObjectName("com.alibaba.cacher:name=hit"));
        mxBean.hitIncr("nihao", 1);
        mxBean.requireIncr("nihao", 2);

        Thread.sleep(1000000);
        //mxBean.reset("nihao");

        //mxBean.requireIncr("testDerby", 88);
        //mxBean.resetAll();
    }

    @Test
    public void testH2() throws InterruptedException, MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        ShootingMXBean mxBean = new H2ShootingMXBeanImpl();

        ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, new ObjectName("com.alibaba.cacher:name=shooting"));
        mxBean.hitIncr("nihao", 1);
        mxBean.requireIncr("nihao", 2);

        Thread.sleep(1000000);
    }

    @Test
    public void testZK() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        ZKShootingMXBeanImpl zkShootingMXBean = new ZKShootingMXBeanImpl("139.129.9.166:2181", "cacher-tester");
        ManagementFactory.getPlatformMBeanServer().registerMBean(zkShootingMXBean, new ObjectName("com.alibaba.cacher:name=shooting"));
        zkShootingMXBean.hitIncr("nihao", 1);
        zkShootingMXBean.hitIncr("tahao", 2);
        zkShootingMXBean.requireIncr("nihao", 88);
        zkShootingMXBean.requireIncr("tahao", 99);
        Utils.delay(100000000);
    }
}
