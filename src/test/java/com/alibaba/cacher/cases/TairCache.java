package com.alibaba.cacher.cases;

import com.taobao.tair.ResultCode;
import com.taobao.tair.etc.KeyValuePack;
import com.taobao.tair.impl.mc.MultiClusterTairManager;
import org.junit.Test;

import java.util.Collections;

/**
 * @author jifang.zjf
 * @since 2017/5/20 下午5:27.
 */
public class TairCache {

    @Test
    public void testInitMultiCluster() {
        MultiClusterTairManager tairManager = new MultiClusterTairManager();
        tairManager.setConfigID("mdbcomm-daily");
        tairManager.setDynamicConfig(true);  // 非常重要，不要忘记
        tairManager.setTimeout(500000);  // 单位为 ms，默认 2000 ms
        tairManager.init();

        KeyValuePack pack = new KeyValuePack("id", "value", (short) 0, 0);
        ResultCode mput = tairManager.mput(162, Collections.singletonList(pack), false);
        //Result<DataEntry> key = mcTairManager.get(909, "key");


        System.out.println(mput);
    }
}
