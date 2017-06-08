package com.alibaba.cacher.support.cache;

import com.alibaba.cacher.ICache;
import com.alibaba.cacher.IObjectSerializer;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.etc.KeyValuePack;
import com.taobao.tair.impl.mc.MultiClusterTairManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author jifang.zjf
 * @since 2017/5/12 下午6:20.
 */
public class TairCache implements ICache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TairCache.class);

    private static final int MGET_PARALLEL_THRESHOLD = 200;

    private MultiClusterTairManager tairManager;

    private int namespace;

    private boolean compress;

    SerializerFeature[] features = new SerializerFeature[]{
            SerializerFeature.WriteClassName,
            SerializerFeature.SkipTransientField,
            SerializerFeature.DisableCircularReferenceDetect
    };

    // 当value都实现了Serializable接口可指定serializer为null;
    private IObjectSerializer serializer;

    public TairCache(String configId, int namespace) {
        this(configId, null, namespace);
    }

    public TairCache(String configId, IObjectSerializer serializer, int namespace) {
        this(configId, namespace, false, serializer, true, -1);
    }

    public TairCache(String configId, int namespace, boolean compress, IObjectSerializer serializer, boolean dynamicConfig, int timeoutMS) {
        tairManager = new MultiClusterTairManager();
        tairManager.setConfigID(configId);
        tairManager.setDynamicConfig(dynamicConfig);
        tairManager.init();
        if (timeoutMS != -1) {
            tairManager.setTimeout(timeoutMS);
        }
        if (serializer != null) {
            this.serializer = serializer;
        }

        this.namespace = namespace;
        this.compress = compress;
    }

    @Override
    public Object read(String key) {
        Object value = null;
        Result<DataEntry> result = tairManager.get(namespace, key);
        if (result.isSuccess()) {
            DataEntry dataEntry = result.getValue();
            if (dataEntry != null) {
                value = getDeserializeObj(dataEntry.getValue());
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
            Consumer<DataEntry> consumer = (DataEntry entry) -> {
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

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        keyValueMap.entrySet().parallelStream().forEach((entry) -> {
            write(entry.getKey(), entry.getValue(), expire);
        });
    }

    public void writeTmp(Map<String, Object> keyValueMap, long expire) {
        List<KeyValuePack> packs = new ArrayList<>(keyValueMap.size());

        short version = 0;  // 强制刷新
        int expireInt = (int) expire;
        keyValueMap.forEach((key, value) -> {
            packs.add(
                    new KeyValuePack(key, getSerializableObj(value), version, expireInt)
            );
        });

//        packs.parallelStream().forEach((pack) ->
//        {
//            tairManager.put()
//        });
        ResultCode resultCode = tairManager.mput(namespace, packs, compress);
        if (!resultCode.isSuccess()) {
            LOGGER.error("tair mput error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
        }
    }


    @Override
    public void remove(String... keys) {
        ResultCode resultCode = tairManager.mdelete(namespace, Arrays.asList(keys));
        if (!resultCode.isSuccess()) {
            LOGGER.error("tair mdelete error, code: {}, message: {}", resultCode.getCode(), resultCode.getMessage());
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


    private boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    private static final class SerializableWrapper implements Serializable {
        private static final long serialVersionUID = 5180629139416743231L;

        private String json;
        private Class<?> type;
    }

}
