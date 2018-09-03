package com.github.cachex.support.cache;

import com.github.jbox.serializer.ISerializer;

import java.util.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-09-03 14:15:00.
 */
public class RedisHelpers {

    /* For Write */
    public static byte[][] toByteArray(Map<String, Object> keyValueMap, ISerializer serializer) {
        byte[][] kvs = new byte[keyValueMap.size() * 2][];
        int index = 0;
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            kvs[index++] = entry.getKey().getBytes();
            kvs[index++] = serializer.serialize(entry.getValue());
        }
        return kvs;
    }

    /* For Read */
    public static byte[][] toByteArray(Collection<String> keys) {
        byte[][] array = new byte[keys.size()][];
        int index = 0;
        for (String str : keys) {
            array[index++] = str.getBytes();
        }
        return array;
    }

    public static Map<String, Object> toObjectMap(Collection<String> keys, List<byte[]> bytesValues, ISerializer serializer) {

        int index = 0;
        Map<String, Object> result = new HashMap<>(keys.size());
        for (String key : keys) {
            Object value = serializer.deserialize(bytesValues.get(index++));
            result.put(key, value);
        }

        return result;
    }
}
