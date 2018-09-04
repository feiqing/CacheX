package com.github.cachex.shooting;

import com.github.cachex.ShootingMXBean;
import com.github.cachex.support.shooting.DerbyShootingMXBeanImpl;
import com.github.cachex.support.shooting.H2ShootingMXBeanImpl;
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

        ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, new ObjectName("com.github.cacherx:name=hit"));
        mxBean.hitIncr("nihao", 1);
        mxBean.reqIncr("nihao", 2);

//        Thread.sleep(1000000);
        //mxBean.reset("nihao");

        //mxBean.reqIncr("testDerby", 88);
        //mxBean.resetAll();
    }

    @Test
    public void testH2() throws InterruptedException, MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        ShootingMXBean mxBean = new H2ShootingMXBeanImpl();

        ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, new ObjectName("com.github.cacherx:name=shooting"));
        mxBean.hitIncr("nihao", 1);
        mxBean.reqIncr("nihao", 2);

//        Thread.sleep(1000000);
    }
}
