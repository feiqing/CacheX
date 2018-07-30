package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.jbox.executor.AsyncExecutor;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.TairManager;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * @author jifang.zjf
 * @since 2017/5/12 下午6:20.
 */
@Slf4j
public class TairCache implements ICache {

    private TairManager tairManager;

    private int namespace;

    private long timeoutMs;

    private Executor mputExecutor;

    public TairCache(TairManager tairManager, int namespace) {
        this(tairManager, namespace, 1000, 10);
    }

    public TairCache(TairManager tairManager, int namespace, int timeoutMS, int nThread) {
        this.namespace = namespace;
        this.timeoutMs = timeoutMS;
        this.tairManager = tairManager;
        if (nThread > 0) {
            AtomicInteger counter = new AtomicInteger(0);
            this.mputExecutor = Executors.newFixedThreadPool(nThread, (runnable) -> {
                Thread thread = new Thread(runnable);
                thread.setName("cachex-tair-mput-thread-" + counter.getAndIncrement());
                // thread.setDaemon(true);

                return thread;
            });
        }
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
        ResultCode result = tairManager.put(namespace, key, (Serializable) value, 0, (int) expire);
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
        BiConsumer<String, Object> writeConsumer = (key, value) -> write(key, value, expire);

        // 并发写入
        if (this.mputExecutor != null) {
            AsyncExecutor<Void> executor = new AsyncExecutor<>();
            for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
                executor.addTask(() -> {
                    write(entry.getKey(), entry.getValue(), expire);
                    return null;
                });
            }
            executor.execute().waiting(timeoutMs, false);
        } else {
            keyValueMap.forEach(writeConsumer);
        }
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
        if (this.mputExecutor != null && this.mputExecutor instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) this.mputExecutor).shutdown();
        }
    }
}
