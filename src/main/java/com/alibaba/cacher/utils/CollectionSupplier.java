package com.alibaba.cacher.utils;

import com.alibaba.cacher.exception.CacherException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static java.util.Collections.*;

/**
 * @author jifang.zjf
 * @since 2017/6/21 下午2:23.
 */
@SuppressWarnings("unchecked")
public class CollectionSupplier {

    private static final Object EMPTY_OBJ = new Object();

    private static final LinkedList EMPTY_LIST = new LinkedList();

    private static final TreeSet EMPTY_SET = new TreeSet();

    private static final Class EMPTY_CLASS = EMPTY_OBJ.getClass();

    private static final ConcurrentMap<Class<?>, Function<Optional<Collection>, Collection>> collectionSuppliers
            = new ConcurrentHashMap<Class<?>, Function<Optional<Collection>, Collection>>() {
        {
            // ******************** //
            // --- need replace until --  //
            // ******************** //

            // empty
            put(emptyList().getClass(), (optional) -> new ArrayList());
            put(emptySet().getClass(), (optional) -> new HashSet());
            put(emptySortedSet().getClass(), (optional) -> new TreeSet());
            put(emptyNavigableSet().getClass(), (optional) -> new HashSet());

            // singleton
            put(singletonList(EMPTY_OBJ).getClass(), (optional) -> new ArrayList());
            put(singleton(EMPTY_OBJ).getClass(), (optional) -> new HashSet());

            // ******************** //
            // --- need convert --- //
            // ******************** //
            // unmodifiable
            put(unmodifiableCollection(EMPTY_SET).getClass(), (optional) -> new ArrayList());
            put(unmodifiableList(EMPTY_LIST).getClass(), (optional) -> new ArrayList());
            put(unmodifiableSet(EMPTY_SET).getClass(), (optional) -> new HashSet());
            put(unmodifiableSortedSet(EMPTY_SET).getClass(), (optional) -> new TreeSet());
            put(unmodifiableNavigableSet(EMPTY_SET).getClass(), (optional) -> new TreeSet());

            // ******************** //
            //  keep what they are  //
            // ******************** //
            // synchronized
            put(synchronizedCollection(EMPTY_LIST).getClass(), (optional) -> optional.orElseGet(() -> synchronizedCollection(new ArrayList())));
            put(synchronizedList(EMPTY_LIST).getClass(), (optional) -> optional.orElseGet(() -> synchronizedList(new ArrayList())));
            put(synchronizedSet(EMPTY_SET).getClass(), (optional) -> optional.orElseGet(() -> synchronizedSet(new HashSet())));
            put(synchronizedNavigableSet(EMPTY_SET).getClass(), (optional) -> optional.orElseGet(() -> synchronizedNavigableSet(new TreeSet())));
            put(synchronizedSortedSet(EMPTY_SET).getClass(), (optional) -> optional.orElseGet(() -> synchronizedSortedSet(new TreeSet())));

            // TODO checked: 全部命中Collection暂时还只是用ArrayList、HashSet、TreeSet、ArrayDeque替换, 后面有需求再反射拿到keyType、valueType
            put(checkedCollection(EMPTY_LIST, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(ArrayList::new));
            put(checkedList(EMPTY_LIST, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(ArrayList::new));
            put(checkedSet(EMPTY_SET, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(HashSet::new));
            put(checkedNavigableSet(EMPTY_SET, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(TreeSet::new));
            put(checkedSortedSet(EMPTY_SET, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(TreeSet::new));
            put(checkedQueue(EMPTY_LIST, EMPTY_CLASS).getClass(), (optional) -> optional.orElseGet(ArrayDeque::new));
        }
    };

    private static final ConcurrentMap<Class<?>, Function<Collection, Collection>> collectionConverters =
            new ConcurrentHashMap<Class<?>, Function<Collection, Collection>>() {
                {
                    // unmodifiable
                    put(unmodifiableCollection(EMPTY_SET).getClass(), Collections::unmodifiableCollection);
                    put(unmodifiableList(EMPTY_LIST).getClass(), (collection) -> unmodifiableList((List) collection));
                    put(unmodifiableSet(EMPTY_SET).getClass(), (collection) -> unmodifiableSet((Set) collection));
                    put(unmodifiableNavigableSet(EMPTY_SET).getClass(), (collection) -> unmodifiableNavigableSet((NavigableSet) collection));
                    put(unmodifiableSortedSet(EMPTY_SET).getClass(), (collection) -> unmodifiableSortedSet((SortedSet) collection));
                }
            };

    /**
     * empty/single -> ArrayList/HashSet
     * unmodifiable -> ArrayList/HashSet
     * synchronized -> origin Collection or synchronized(ArrayList/HashSet/TreeSet)
     * checked -> origin Collection or ArrayList/HashSet/TreeSet/ArrayDeque
     * other -> class.newInstance()
     *
     * @param type
     * @return
     */
    public static Collection newInstance(Class<?> type) {
        Function<Optional<Collection>, Collection> supplier = getSupplier(type);
        return supplier.apply(Optional.empty());
    }

    /**
     * ditto.
     *
     * @param type
     * @param fromMethodCollection
     * @return
     */
    public static Collection newInstance(Class<?> type, Collection fromMethodCollection) {
        Function<Optional<Collection>, Collection> supplier = getSupplier(type);
        return supplier.apply(Optional.of(fromMethodCollection));
    }

    /**
     * unmodifiable type Collection -> unmodifiableCollection
     * other -> origin Collection
     *
     * @param type
     * @param instance
     * @return
     */
    public static Collection convertInstanceType(Class<?> type, Collection instance) {
        Function<Collection, Collection> converter = collectionConverters.getOrDefault(type, collection -> collection);
        return converter.apply(instance);
    }

    private static Function<Optional<Collection>, Collection> getSupplier(Class<?> type) {
        return collectionSuppliers.getOrDefault(type, (optional) -> {
            try {
                return (Collection) type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new CacherException("could not invoke collection: " + type + "'s no param (default) constructor", e);
            }
        });
    }
}
