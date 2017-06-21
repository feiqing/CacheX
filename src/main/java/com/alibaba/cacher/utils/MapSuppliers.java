package com.alibaba.cacher.utils;

import com.alibaba.cacher.exception.CacherException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static java.util.Collections.*;

/**
 * @author jifang.zjf
 * @since 2017/6/21 上午11:43.
 */
@SuppressWarnings("unchecked")
public class MapSuppliers {

    private static final Object EMPTY_OBJ = new Object();

    private static final TreeMap EMPTY_MAP = new TreeMap();

    private static final Class EMPTY_CLASS = EMPTY_OBJ.getClass();

    private static final ConcurrentMap<Class<?>, Function<Optional<Map>, Map>> mapSuppliers
            = new ConcurrentHashMap<Class<?>, Function<Optional<Map>, Map>>() {
        {
            // ******************** //
            // --- need replace until --  //
            // ******************** //
            // empty
            put(emptyMap().getClass(), (optional) -> new HashMap());
            put(emptyNavigableMap().getClass(), (optional) -> new TreeMap());
            put(emptySortedMap().getClass(), (optional) -> new TreeMap());

            // singleton
            put(singletonMap(EMPTY_OBJ, EMPTY_OBJ).getClass(), (optional) -> new HashMap());

            // ******************** //
            // --- need convert --- //
            // ******************** //

            // unmodifiable
            put(unmodifiableMap(EMPTY_MAP).getClass(), (optional) -> new HashMap());
            put(unmodifiableNavigableMap(EMPTY_MAP).getClass(), (optional) -> new TreeMap());
            put(unmodifiableSortedMap(EMPTY_MAP).getClass(), (optional) -> new TreeMap());

            // ******************** //
            //  keep what they are  //
            // ******************** //
            // synchronized
            put(synchronizedMap(EMPTY_MAP).getClass(), (optional) -> optional.orElseGet(() -> synchronizedMap(new HashMap())));
            put(synchronizedNavigableMap(EMPTY_MAP).getClass(), (optional) -> optional.orElseGet(() -> synchronizedNavigableMap(new TreeMap<>())));
            put(synchronizedSortedMap(EMPTY_MAP).getClass(), (optional) -> optional.orElseGet(() -> synchronizedSortedMap(new TreeMap<>())));

            // TODO checked: 全部命中MapConvert暂时还只是用HashMap、TreeMap替换, 后面有需求再反射拿到keyType、valueType
            put(checkedMap(EMPTY_MAP, EMPTY_CLASS, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(HashMap::new));
            put(checkedNavigableMap(EMPTY_MAP, EMPTY_CLASS, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(TreeMap::new));
            put(checkedSortedMap(EMPTY_MAP, EMPTY_CLASS, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(TreeMap::new));
        }
    };

    private static final ConcurrentMap<Class<?>, Function<Map, Map>> mapConverters = new ConcurrentHashMap<Class<?>, Function<Map, Map>>() {
        {
            // unmodifiable
            put(unmodifiableMap(EMPTY_MAP).getClass(), Collections::unmodifiableMap);
            put(unmodifiableNavigableMap(EMPTY_MAP).getClass(), map -> unmodifiableNavigableMap((NavigableMap) map));
            put(unmodifiableSortedMap(EMPTY_MAP).getClass(), map -> unmodifiableSortedMap((SortedMap) map));
        }
    };

    /**
     * emptyMap/singletonMap -> HashMap
     * unmodifiableMap -> HashMap
     * synchronizedMap -> origin Map or synchronized(HashMap/TreeMap)
     * checkedMap -> origin Map or HashMap/TreeMap
     * other -> class.newInstance()
     *
     * @param type
     * @return
     */
    public static Map newInstance(Class<?> type) {
        return getSupplier(type).apply(Optional.empty());
    }

    /**
     * ditto.
     *
     * @param type
     * @param fromMethodMap
     * @return
     */
    public static Map newInstance(Class<?> type, Map fromMethodMap) {
        return getSupplier(type).apply(Optional.of(fromMethodMap));
    }

    /**
     * unmodifiable type Map -> unmodifiableMap Instance
     * other -> origin map
     *
     * @param type
     * @param instance
     * @return
     */
    public static Map convertInstanceType(Class<?> type, Map instance) {
        Function<Map, Map> converter = mapConverters.getOrDefault(type, map -> map);
        return converter.apply(instance);
    }

    private static Function<Optional<Map>, Map> getSupplier(Class<?> type) {
        return mapSuppliers.getOrDefault(type, (optional) -> {
            try {
                return (Map) type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new CacherException("could not invoke map: " + type + "'s no param (default) constructor", e);
            }
        });
    }
}
