package com.github.cachex.reader;

import com.github.cachex.ShootingMXBean;
import com.github.cachex.core.CacheXConfig;
import com.github.cachex.domain.CacheReadResult;
import com.github.cachex.domain.CacheXAnnoHolder;
import com.github.cachex.domain.CacheXMethodHolder;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.manager.CacheManager;
import com.github.cachex.utils.PatternGenerator;
import com.github.cachex.utils.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jifang
 * @since 2016/11/5 下午3:11.
 */
@Singleton
public class MultiCacheReader extends AbstractCacheReader {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private CacheXConfig config;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheXAnnoHolder cacheXAnnoHolder, CacheXMethodHolder cacheXMethodHolder, Invoker invoker, boolean needWrite) throws Throwable {
        // compose keys
        Map[] pair = KeyGenerator.generateMultiKey(cacheXAnnoHolder, invoker.getArgs());
        Map<String, Object> key2MultiEntry = pair[1];

        // request cache
        Set<String> keys = key2MultiEntry.keySet();
        CacheReadResult cacheReadResult = cacheManager.readBatch(cacheXAnnoHolder.getCache(), keys);
        doRecord(cacheReadResult, cacheXAnnoHolder);

        Object result;
        // have miss keys : part hit || all not hit
        if (!cacheReadResult.getMissKeySet().isEmpty()) {
            result = handlePartHit(invoker, cacheReadResult, cacheXAnnoHolder, cacheXMethodHolder, pair, needWrite);
        }
        // no miss keys : all hit || empty key
        else {
            Map<String, Object> keyValueMap = cacheReadResult.getHitKeyMap();
            result = handleFullHit(invoker, keyValueMap, cacheXMethodHolder, key2MultiEntry);
        }

        return result;
    }

    private Object handlePartHit(Invoker invoker, CacheReadResult cacheReadResult,
                                 CacheXAnnoHolder cacheXAnnoHolder, CacheXMethodHolder cacheXMethodHolder,
                                 Map[] pair, boolean needWrite) throws Throwable {

        Map<Object, String> multiEntry2Key = pair[0];
        Map<String, Object> key2MultiEntry = pair[1];

        Set<String> missKeys = cacheReadResult.getMissKeySet();
        Map<String, Object> hitKeyValueMap = cacheReadResult.getHitKeyMap();

        // 用未命中的keys调用方法
        Object[] missArgs = toMissArgs(missKeys, key2MultiEntry, invoker.getArgs(), cacheXAnnoHolder.getMultiIndex());
        Object proceed = doLogInvoke(() -> invoker.proceed(missArgs));

        Object result;
        if (proceed != null) {
            Class<?> returnType = proceed.getClass();
            cacheXMethodHolder.setReturnType(returnType);
            if (Map.class.isAssignableFrom(returnType)) {
                Map proceedEntryValueMap = (Map) proceed;

                // @since 1.5.4 为了兼容@CachedGet注解, 客户端缓存
                if (needWrite) {
                    // 将方法调用返回的map转换成key_value_map写入Cache
                    Map<String, Object> keyValueMap = KeyValueUtils.mapToKeyValue(proceedEntryValueMap, missKeys, multiEntry2Key, config.getPrevent());
                    cacheManager.writeBatch(cacheXAnnoHolder.getCache(), keyValueMap, cacheXAnnoHolder.getExpire());
                }
                // 将方法调用返回的map与从Cache中读取的key_value_map合并返回
                result = ResultUtils.mergeMap(returnType, proceedEntryValueMap, key2MultiEntry, hitKeyValueMap);
            } else {
                Collection proceedCollection = asCollection(proceed, returnType);

                // @since 1.5.4 为了兼容@CachedGet注解, 客户端缓存
                if (needWrite) {
                    // 将方法调用返回的collection转换成key_value_map写入Cache
                    Map<String, Object> keyValueMap = KeyValueUtils.collectionToKeyValue(proceedCollection, cacheXAnnoHolder.getId(), missKeys, multiEntry2Key, config.getPrevent());
                    cacheManager.writeBatch(cacheXAnnoHolder.getCache(), keyValueMap, cacheXAnnoHolder.getExpire());
                }
                // 将方法调用返回的collection与从Cache中读取的key_value_map合并返回
                Collection resultCollection = ResultUtils.mergeCollection(returnType, proceedCollection, hitKeyValueMap);
                result = asType(resultCollection, returnType);
            }
        } else {
            // read as full shooting
            result = handleFullHit(invoker, hitKeyValueMap, cacheXMethodHolder, key2MultiEntry);
        }

        return result;
    }

    private Object asType(Collection collection, Class<?> returnType) {
        if (Collection.class.isAssignableFrom(returnType)) {
            return collection;
        }

        return collection.toArray();
    }

    private Collection asCollection(Object proceed, Class<?> returnType) {
        if (Collection.class.isAssignableFrom(returnType)) {
            return (Collection) proceed;
        }

        return Arrays.asList((Object[]) proceed);
    }

    private Object handleFullHit(Invoker invoker, Map<String, Object> keyValueMap,
                                 CacheXMethodHolder cacheXMethodHolder, Map<String, Object> key2Id) throws Throwable {

        Object result;
        Class<?> returnType = cacheXMethodHolder.getReturnType();

        // when method return type not cached. case: full shooting when application restart
        if (returnType == null) {
            result = doLogInvoke(invoker::proceed);

            // catch return type for next time
            if (result != null) {
                cacheXMethodHolder.setReturnType(result.getClass());
            }
        } else {
            if (cacheXMethodHolder.isCollection()) {
                result = ResultUtils.toCollection(returnType, keyValueMap);
            } else {
                result = ResultUtils.toMap(returnType, key2Id, keyValueMap);
            }
        }

        return result;
    }

    private Object[] toMissArgs(Set<String> missKeys, Map<String, Object> keyIdMap,
                                Object[] args, int multiIndex) {

        List<Object> missedMultiEntries = missKeys.stream()
                .map(keyIdMap::get)
                .collect(Collectors.toList());

        Class<?> multiArgType = args[multiIndex].getClass();

        // 对将Map作为CacheKey的支持就到这儿了, 不会再继续下去...
        Addables.Addable addable = Addables.newAddable(multiArgType, missedMultiEntries.size());
        args[multiIndex] = addable.addAll(missedMultiEntries).getResult();

        return args;
    }

    private void doRecord(CacheReadResult cacheReadResult, CacheXAnnoHolder cacheXAnnoHolder) {
        Set<String> missKeys = cacheReadResult.getMissKeySet();

        // 计数
        int hitCount = cacheReadResult.getHitKeyMap().size();
        int totalCount = hitCount + missKeys.size();
        CacheXLogger.info("multi cache hit rate: {}/{}, missed keys: {}",
                hitCount, totalCount, missKeys);

        if (this.shootingMXBean != null) {
            // 分组模板
            String pattern = PatternGenerator.generatePattern(cacheXAnnoHolder);

            this.shootingMXBean.hitIncr(pattern, hitCount);
            this.shootingMXBean.reqIncr(pattern, totalCount);
        }
    }
}
