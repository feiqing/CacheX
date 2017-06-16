package com.alibaba.cacher.support.cache;

import com.alibaba.cacher.ICache;
import com.alibaba.fastjson.JSONObject;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.impl.mc.MultiClusterTairManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author jifang.zjf
 * @since 2017/5/12 下午6:20.
 */
public class TairCache implements ICache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TairCache.class);

    private static final int MGET_PARALLEL_THRESHOLD = 100;

    private MultiClusterTairManager tairManager;

    private int namespace;

    private Executor mputExecutor;

    public TairCache(String configId, int namespace) {
        this(configId, namespace, true, 500, 50);
    }

    public TairCache(String configId, int namespace, boolean dynamicConfig, int timeoutMS, int nThread) {
        this.namespace = namespace;
        this.tairManager = new MultiClusterTairManager();
        tairManager.setConfigID(configId);
        tairManager.setDynamicConfig(dynamicConfig);
        tairManager.setTimeout(timeoutMS);
        tairManager.init();

        if (nThread > 0) {
            AtomicInteger counter = new AtomicInteger(0);
            this.mputExecutor = Executors.newFixedThreadPool(nThread, (runnable) -> {
                Thread thread = new Thread(runnable);
                thread.setName("cacher-tair-mput-thread-" + counter.getAndIncrement());
                // thread.setDaemon(true);

                return thread;
            });
        }
    }

    @Override
    public Object read(String key) {
        Object value = null;
        Result<DataEntry> result = tairManager.get(namespace, key);
        if (result.isSuccess()) {
            DataEntry dataEntry = result.getValue();
            if (dataEntry != null) {
                value = getDeserializeObj(dataEntry.getValue());
                if (value == null) {
                    throw new RuntimeException("func");
                }
            }
        } else {
            ResultCode resultCode = result.getRc();
            LOGGER.error("tair get error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
        }

        return value;
    }

    @Override
    public void write(String key, Object value, long expire) {
        ResultCode result = tairManager.put(namespace, key, getSerializableObj(value), 0, (int) expire);
        if (!result.isSuccess()) {
            LOGGER.error("tair put error, code: {}, message: {}", result.getCode(), result.getMessage());
        }
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Result<List<DataEntry>> results = tairManager.mget(namespace, new ArrayList<>(keys));

        Map<String, Object> resultMap;
        List<DataEntry> entries;
        if ((entries = results.getValue()) != null) {
            resultMap = new HashMap<>(entries.size());
            Consumer<DataEntry> consumer = (entry) -> {
                resultMap.put((String) entry.getKey(), getDeserializeObj(entry.getValue()));
            };

            if (entries.size() >= MGET_PARALLEL_THRESHOLD) {
                entries.parallelStream().forEach(consumer);
            } else {
                entries.forEach(consumer);
            }
        } else {
            resultMap = Collections.emptyMap();

            ResultCode resultCode = results.getRc();
            LOGGER.error("tair mget error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
        }

        return resultMap;
    }


    // tips: this.tairManager.mput(); X
    // 新版本的Tair不再支持mput命令, 因此当key数量过多时建议使用并发方式 Write Tair
    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        BiConsumer<String, Object> writeConsumer = (key, value) -> write(key, value, expire);

        // 并发写入
        if (this.mputExecutor != null) {
            List<CompletableFuture<Void>> futures = new ArrayList<>(keyValueMap.size());

            keyValueMap.forEach((key, value) -> {
                CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                    writeConsumer.accept(key, value);
                    return null;
                }, this.mputExecutor);

                futures.add(future);
            });

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
        } else {
            keyValueMap.forEach(writeConsumer);
        }
    }

    @Override
    public void remove(String... keys) {
        ResultCode resultCode = tairManager.mdelete(namespace, Arrays.asList(keys));
        if (!resultCode.isSuccess()) {
            LOGGER.error("tair mdelete error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
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

    private Serializable getSerializableObj(Object obj) {
        if (!(obj instanceof Serializable)) {

            SerializableWrapper wrapper = new SerializableWrapper();
            wrapper.json = JSONObject.toJSONString(obj);
            wrapper.type = obj.getClass();

            obj = wrapper;
        }

        return (Serializable) obj;
    }

    private Object getDeserializeObj(Object obj) {
        if (obj instanceof SerializableWrapper) {
            SerializableWrapper wrapper = (SerializableWrapper) obj;

            obj = JSONObject.parseObject(wrapper.json, wrapper.type);
        }

        return obj;
    }

    private static final class SerializableWrapper implements Serializable {

        private static final long serialVersionUID = 5180629139416743231L;

        private String json;

        private Class<?> type;
    }
}
