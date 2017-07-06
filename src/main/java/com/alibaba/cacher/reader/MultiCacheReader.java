package com.alibaba.cacher.reader;

import com.alibaba.cacher.ShootingMXBean;
import com.alibaba.cacher.core.Config;
import com.alibaba.cacher.di.Inject;
import com.alibaba.cacher.di.Singleton;
import com.alibaba.cacher.domain.BatchReadResult;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.CacheMethodHolder;
import com.alibaba.cacher.invoker.Invoker;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.supplier.CollectionSupplier;
import com.alibaba.cacher.supplier.PatternSupplier;
import com.alibaba.cacher.utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jifang
 * @since 2016/11/5 下午3:11.
 */
@Singleton
@SuppressWarnings("unchecked")
public class MultiCacheReader extends AbstractCacheReader {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private Config config;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder, Invoker invoker, boolean needWrite) throws Throwable {
        // compose keys
        Map[] pair = KeyGenerators.generateMultiKey(cacheKeyHolder, invoker.getArgs());
        Map<String, Object> keyIdMap = pair[1];

        // request cache
        Set<String> keys = keyIdMap.keySet();
        BatchReadResult batchReadResult = cacheManager.readBatch(cacheKeyHolder.getCache(), keys);
        doRecord(batchReadResult, cacheKeyHolder);

        Object result;
        // have miss keys : part hit || all not hit
        if (!batchReadResult.getMissKeys().isEmpty()) {
            result = handlePartHit(invoker, batchReadResult, cacheKeyHolder, cacheMethodHolder, pair, needWrite);
        }
        // no miss keys : all hit || empty key
        else {
            Map<String, Object> keyValueMap = batchReadResult.getHitKeyValueMap();
            result = handleFullHit(invoker, keyValueMap, cacheMethodHolder, keyIdMap);
        }

        return result;
    }

    private Object handlePartHit(Invoker invoker, BatchReadResult batchReadResult,
                                 CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder,
                                 Map[] pair, boolean needWrite) throws Throwable {

        Map<Object, String> id2Key = pair[0];
        Map<String, Object> key2Id = pair[1];

        Set<String> missKeys = batchReadResult.getMissKeys();
        Map<String, Object> hitKeyValueMap = batchReadResult.getHitKeyValueMap();

        // 用未命中的keys调用方法
        Object[] missArgs = toMissArgs(missKeys, key2Id, invoker.getArgs(), cacheKeyHolder.getMultiIndex());
        Object proceed = doLogInvoke(() -> invoker.proceed(missArgs));

        Object result;
        if (proceed != null) {
            Class<?> returnType = proceed.getClass();
            cacheMethodHolder.setReturnType(returnType);
            if (Map.class.isAssignableFrom(returnType)) {
                Map proceedIdValueMap = (Map) proceed;

                // @since 1.5.4 为了兼容@CachedGet注解, 客户端缓存
                if (needWrite) {
                    // 将方法调用返回的map转换成key_value_map写入Cache
                    Map<String, Object> keyValueMap = KVConvertUtils.mapToKeyValue(proceedIdValueMap, missKeys, id2Key, config.isPreventBreakdown());
                    cacheManager.writeBatch(cacheKeyHolder.getCache(), keyValueMap, cacheKeyHolder.getExpire());
                }
                // 将方法调用返回的map与从Cache中读取的key_value_map合并返回
                result = ResultMergeUtils.mergeMap(key2Id, returnType, proceedIdValueMap, hitKeyValueMap);
            } else {
                Collection proceedCollection = (Collection) proceed;

                // @since 1.5.4 为了兼容@CachedGet注解, 客户端缓存
                if (needWrite) {
                    // 将方法调用返回的collection转换成key_value_map写入Cache
                    Map<String, Object> keyValueMap = KVConvertUtils.collectionToKeyValue(proceedCollection, cacheKeyHolder.getId(), missKeys, id2Key, config.isPreventBreakdown());
                    cacheManager.writeBatch(cacheKeyHolder.getCache(), keyValueMap, cacheKeyHolder.getExpire());
                }
                // 将方法调用返回的collection与从Cache中读取的key_value_map合并返回
                result = ResultMergeUtils.mergeCollection(returnType, proceedCollection, hitKeyValueMap);
            }
        } else {
            // read as full shooting
            result = handleFullHit(invoker, hitKeyValueMap, cacheMethodHolder, key2Id);
        }

        return result;
    }

    private Object handleFullHit(Invoker invoker, Map<String, Object> keyValueMap,
                                 CacheMethodHolder cacheMethodHolder, Map<String, Object> key2Id) throws Throwable {

        Object result;
        Class<?> returnType = cacheMethodHolder.getReturnType();

        // when method return type not cached. case: full shooting when application restart
        if (returnType == null) {
            result = doLogInvoke(invoker::proceed);

            // catch return type for next time
            if (result != null) {
                cacheMethodHolder.setReturnType(result.getClass());
            }
        } else {
            if (cacheMethodHolder.isCollection()) {
                result = ResultConvertUtils.toCollection(returnType, keyValueMap);
            } else {
                result = ResultConvertUtils.toMap(key2Id, returnType, keyValueMap);
            }
        }

        return result;
    }

    private Object[] toMissArgs(Set<String> missKeys, Map<String, Object> keyIdMap,
                                Object[] args, int batchIndex) {

        List<Object> missIds = missKeys.stream()
                .map(keyIdMap::get)
                .collect(Collectors.toList());

        Collection collection = CollectionSupplier.newInstance(args[batchIndex].getClass());
        collection.addAll(missIds);
        args[batchIndex] = collection;

        return args;
    }

    private void doRecord(BatchReadResult batchReadResult, CacheKeyHolder cacheKeyHolder) {
        Set<String> missKeys = batchReadResult.getMissKeys();

        // 计数
        int hitCount = batchReadResult.getHitKeyValueMap().size();
        int totalCount = hitCount + missKeys.size();
        LOGGER.info("multi cache hit rate: {}/{}, missed keys: {}",
                hitCount, totalCount, missKeys);

        if (this.shootingMXBean != null) {
            // 分组模板
            String pattern = PatternSupplier.getPattern(cacheKeyHolder);

            this.shootingMXBean.hitIncr(pattern, hitCount);
            this.shootingMXBean.requireIncr(pattern, totalCount);
        }
    }
}
