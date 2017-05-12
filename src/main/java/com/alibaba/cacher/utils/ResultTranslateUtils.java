package com.alibaba.cacher.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * @author jifang
 * @since 2016/11/18 下午4:34.
 */
public class ResultTranslateUtils {

    public static Collection toCollection(Class<?> returnType, Collection values)
            throws
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {

        return (Collection) returnType.getConstructor(Collection.class).newInstance(values);
    }

    @SuppressWarnings("unchecked")
    public static Map toMap(Class<?> returnType, Map<String, Object> keyValueMap, Map<String, Object> keyIdMap)
            throws
            IllegalAccessException, InstantiationException {

        Map map = (Map) returnType.newInstance();
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            String key = entry.getKey();

            Object id = keyIdMap.get(key);
            Object value = entry.getValue();

            map.put(id, value);
        }

        return map;
    }
}
