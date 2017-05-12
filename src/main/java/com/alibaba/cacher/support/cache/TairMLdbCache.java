package com.alibaba.cacher.support.cache;

import com.alibaba.cacher.ICache;
import com.alibaba.cacher.IObjectSerializer;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.etc.KeyValuePack;
import com.taobao.tair.impl.mc.MultiClusterTairManager;
import com.taobao.tair.impl.mc.external.tc.TCMultiClusterTairManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * @author jifang.zjf
 * @since 2017/5/12 下午6:20.
 */
public class TairMLdbCache implements ICache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TairMLdbCache.class);

    private MultiClusterTairManager tairManager;

    private int namespace;

    private boolean compress;

    // 当value都实现了Serializable接口可指定serializer为null;
    private IObjectSerializer serializer;

    public TairMLdbCache(String configId, int namespace) {
        this(configId, null, namespace);
    }

    public TairMLdbCache(String configId, IObjectSerializer serializer, int namespace) {
        this(configId, namespace, serializer, true, -1);
    }

    public TairMLdbCache(String configId, int namespace, IObjectSerializer serializer, boolean dynamicConfig, int timeoutMS) {
        tairManager = new TCMultiClusterTairManager();
        tairManager.setConfigID(configId);
        tairManager.setDynamicConfig(dynamicConfig);
        if (timeoutMS != -1) {
            tairManager.setTimeout(timeoutMS);
        }
        if (serializer != null) {
            this.serializer = serializer;
        }

        this.namespace = namespace;
    }

    // TODO
    private void loggerError(Result result) {
        if (result != null && !result.isSuccess()) {
            loggerError(result.getRc());
        } else {
            // logger null
        }
    }

    // TODO
    private void loggerError(ResultCode resultCode) {
        if (!resultCode.isSuccess()) {
            LOGGER.error("error, code:{}, message:{}", resultCode.getCode(), resultCode.getMessage());
        }
    }

    @Override
    public Object read(String key) {
        Result<DataEntry> result = tairManager.get(namespace, key);
        if (result != null && result.isSuccess()) {
            return getDeserializedObj(result.getValue());
        } else {
            loggerError(result);
        }

        return null;
    }

    @Override
    public void write(String key, Object value, long expire) {
        ResultCode resultCode;
        if (value instanceof Serializable) {
            resultCode = tairManager.put(namespace, key, (Serializable) value);
        } else {
            resultCode = tairManager.put(namespace, key, new DataWrapper(value, serializer));
        }
        loggerError(resultCode);
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Result<List<DataEntry>> results;
        if (keys instanceof List) {
            results = tairManager.mget(namespace, (List<?>) keys);
        } else {
            results = tairManager.mget(namespace, new ArrayList<>(keys));
        }

        Map<String, Object> resultMap;
        if (results != null && results.isSuccess()) {
            resultMap = new HashMap<>();

            List<DataEntry> values = results.getValue();
            int index = 0;
            for (String key : keys) {
                resultMap.put(key, getDeserializedObj(values.get(index++)));
            }
        } else {
            resultMap = Collections.emptyMap();
            loggerError(results);
        }

        return resultMap;
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        List<KeyValuePack> keyValues = new ArrayList<>(keyValueMap.size());
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof Serializable)) {
                value = new DataWrapper(value, serializer);
            }

            keyValues.add(new KeyValuePack(key, value));
        }

        loggerError(tairManager.mput(namespace, keyValues, compress));
    }

    @Override
    public void remove(String... keys) {
        loggerError(tairManager.mdelete(namespace, Arrays.asList(keys)));
    }

    private Object getDeserializedObj(DataEntry dataEntry) {
        Object value = dataEntry.getValue();
        if (value instanceof DataWrapper) {
            value = ((DataWrapper) value).getDeserializeObj();
        }
        return value;
    }

    private static final class DataWrapper implements Serializable {

        private byte[] bytes;

        private IObjectSerializer serializer;

        DataWrapper(Object needSerializeObj, IObjectSerializer serializer) {
            this.serializer = serializer;
            this.bytes = serializer.serialize(needSerializeObj);
        }

        Object getDeserializeObj() {
            return this.serializer.deserialize(this.bytes);
        }
    }
}
