package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.jbox.executor.AsyncJobExecutor;
import com.github.jbox.executor.ExecutorManager;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.TairManager;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author jifang.zjf
 * @since 2017/5/12 下午6:20.
 */
@Slf4j
public class TairCache implements ICache {

    private TairManager tairManager;

    private int namespace;

    private ExecutorService worker;

    public TairCache(TairManager tairManager, int namespace) {
        this(tairManager, namespace, ExecutorManager.newFixedThreadPool("CacheXTairWriter", Runtime.getRuntime().availableProcessors()));
    }

    public TairCache(TairManager tairManager, int namespace, ExecutorService worker) {
        this.namespace = namespace;
        this.tairManager = tairManager;
        this.worker = worker;
    }

    @Override
    public Object read(String key) {
        Result<DataEntry> result = tairManager.get(namespace, key);
        DataEntry dataEntry;
        if (result.isSuccess() && (dataEntry = result.getValue()) != null) {
            return dataEntry.getValue();
        } else {
            ResultCode resultCode = result.getRc();
            log.error("tair get error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
        }

        return null;
    }

    @Override
    public void write(String key, Object value, long expire) {
        ResultCode result = tairManager.put(namespace, key, (Serializable) value, 0, (int) (expire / 1000));
        if (!result.isSuccess()) {
            log.error("tair put error, code: {}, message: {}", result.getCode(), result.getMessage());
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Result<List<DataEntry>> results = tairManager.mget(namespace, new ArrayList<>(keys));

        List<DataEntry> entries;
        if ((entries = results.getValue()) != null) {
            Map<String, Object> resultMap = new HashMap<>(entries.size());
            entries.forEach((entry) -> resultMap.put((String) entry.getKey(), entry.getValue()));

            return resultMap;
        } else {
            ResultCode resultCode = results.getRc();
            log.error("tair mget error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
        }

        return Collections.emptyMap();
    }


    // tips: this.tairManager.mput(); X
    // 新版本的Tair不再支持mput命令, 因此当key数量过多时建议使用并发方式 Write Tair
    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        if (this.worker == null) {
            keyValueMap.forEach((key, value) -> this.write(key, value, expire));
            return;
        }

        // 并发写入
        AsyncJobExecutor job = new AsyncJobExecutor(worker);
        keyValueMap.forEach((key, value) -> job.addTask(() -> {
            this.write(key, value, expire);
            return null;
        }));
        job.execute().waiting();
    }

    @Override
    public void remove(String... keys) {
        ResultCode resultCode = tairManager.mdelete(namespace, Arrays.asList(keys));
        if (!resultCode.isSuccess()) {
            log.error("tair mdelete error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
        }
    }

    @PreDestroy
    public void tearDown() {
        if (this.tairManager != null) {
            this.tairManager.close();
        }
    }
}
