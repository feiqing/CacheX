package com.alibaba.cacher.utils;

import com.alibaba.cacher.IObjectSerializer;

import java.util.*;

/**
 * @author jifang
 * @since 2017/4/7 上午10:47.
 */
public class SerializeUtils {

    /* For Write */
    public static byte[][] toByteArray(Map<String, Object> keyValueMap, IObjectSerializer serializer) {
        byte[][] kvs = new byte[keyValueMap.size() * 2][];
        int index = 0;
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            kvs[index++] = entry.getKey().getBytes();
            kvs[index++] = serializer.serialize(entry.getValue());
        }
        return kvs;
    }

    public static Map<byte[], byte[]> toByteMap(Map<String, Object> keyValues, IObjectSerializer serializer) {
        Map<byte[], byte[]> keyValueBytes = new HashMap<>(keyValues.size());
        for (Map.Entry<String, Object> entry : keyValues.entrySet()) {

            byte[] keyBytes = entry.getKey().getBytes();
            byte[] valueBytes = serializer.serialize(entry.getValue());

            keyValueBytes.put(keyBytes, valueBytes);
        }
        return keyValueBytes;
    }

    public static List<Map<byte[], byte[]>> toByteMap(Map<String, Object> keyValues, IObjectSerializer serializer, int limit) {
        List<Map<byte[], byte[]>> maps = new LinkedList<>();

        int count = 0;
        Map<byte[], byte[]> keyValueBytes = new HashMap<>(limit);
        for (Map.Entry<String, Object> entry : keyValues.entrySet()) {

            byte[] keyBytes = entry.getKey().getBytes();
            byte[] valueBytes = serializer.serialize(entry.getValue());
            keyValueBytes.put(keyBytes, valueBytes);
            ++count;

            if (count == limit) {
                count = 0;
                maps.add(keyValueBytes);
                keyValueBytes = new HashMap<>(limit);
            }
        }
        if (count != 0) {
            maps.add(keyValueBytes);
        }

        return maps;
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

    public static Map<String, Object> toObjectMap(Collection<String> keys, List<byte[]> bytesValues, IObjectSerializer serializer) {

        int index = 0;
        Map<String, Object> result = new HashMap<>(keys.size());
        for (String key : keys) {
            Object value = serializer.deserialize(bytesValues.get(index++));
            result.put(key, value);
        }

        return result;
    }
}
