package com.alibaba.cacher.reader;

import com.alibaba.cacher.Cached;
import com.alibaba.cacher.config.Inject;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.domain.BatchReadResult;
import com.alibaba.cacher.domain.CacheKeyHolder;
import com.alibaba.cacher.domain.MethodInfoHolder;
import com.alibaba.cacher.shooting.ShootingMXBean;
import com.alibaba.cacher.manager.CacheManager;
import com.alibaba.cacher.utils.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author jifang
 * @since 2016/11/5 下午3:11.
 */
@Singleton
@SuppressWarnings("unchecked")
public class MultiCacheReader implements CacheReader {

    @Inject
    private CacheManager cacheManager;

    @Inject(optional = true)
    private ShootingMXBean shootingMXBean;

    @Override
    public Object read(CacheKeyHolder holder, Cached cached, ProceedingJoinPoint pjp, MethodInfoHolder ret) throws Throwable {

        // compose keys
        Map[] pair = KeysCombineUtil.toMultiKey(holder, cached.separator(), pjp.getArgs());
        String keyPattern = KeyPatternsCombineUtil.getKeyPattern(holder, cached.separator());

        Map<String, Object> keyIdMap = pair[1];

        // request cache
        Set<String> keys = keyIdMap.keySet();
        BatchReadResult batchReadResult = cacheManager.readBatch(cached.cache(), keys);
        doRecord(batchReadResult, keyPattern);

        Object result;
        // have miss keys : part shooting || all not shooting
        if (batchReadResult.getMissKeys().size() > 0) {
            result = handlePartHit(pjp, batchReadResult, holder, ret, cached, pair);
        }
        // no miss keys : all hits || empty key
        else {
            Map<String, Object> keyValueMap = batchReadResult.getHitKeyValueMap();
            result = handleFullHit(pjp, keyValueMap, ret, keyIdMap);
        }

        return result;
    }

    private Object handlePartHit(ProceedingJoinPoint pjp, BatchReadResult batchReadResult,
                                 CacheKeyHolder rule, MethodInfoHolder ret, Cached cached,
                                 Map[] pair) throws Throwable {

        Map<Object, String> id_key = pair[0];
        Map<String, Object> key_id = pair[1];

        Set<String> missKeys = batchReadResult.getMissKeys();
        Map<String, Object> hitKeyValueMap = batchReadResult.getHitKeyValueMap();

        // invoke method use missed keys
        Object[] missArgs = toMissArgs(missKeys, key_id, pjp.getArgs(), rule.getMultiIndex());
        long start = 0;
        if (LOGGER.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }
        Object proceed = pjp.proceed(missArgs);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("method invoke total cost [{}] ms", (System.currentTimeMillis() - start));
        }

        Object result;
        if (proceed != null) {
            Class<?> returnType = proceed.getClass();
            ret.setType(returnType);
            // 兼容各种内部类

            if (Map.class.isAssignableFrom(returnType)) {
                Map proceedIdValueMap = (Map) proceed;

                Map<String, Object> keyValueMap = KeyValueConverts.idValueMap2KeyValue(proceedIdValueMap, id_key);
                // write proceed map to cache
                cacheManager.writeBatch(cached.cache(), keyValueMap, cached.expire());

                // merge map by key-id-map order
                result = ResultMergeUtils.mapMerge(key_id, returnType, proceedIdValueMap, hitKeyValueMap);
            } else {
                Collection proceedCollection = (Collection) proceed;
                String idExp = rule.getIdentifier();

                Map<String, Object> keyValueMap = KeyValueConverts.collection2KeyValue(proceedCollection, idExp, id_key);

                cacheManager.writeBatch(cached.cache(), keyValueMap, cached.expire());

                result = ResultMergeUtils.collectionMerge(key_id.keySet(), returnType, keyValueMap, hitKeyValueMap);
            }
        } else {
            // read as full shooting
            result = handleFullHit(pjp, hitKeyValueMap, ret, key_id);
        }

        return result;
    }

    private Object handleFullHit(ProceedingJoinPoint pjp, Map<String, Object> keyValueMap,
                                 MethodInfoHolder ret, Map<String, Object> keyIdMap)
            throws Throwable {

        Object result;
        Class<?> returnType = ret.getType();

        // when method return type not cached. case: full shooting when application restart
        if (returnType == null) {

            long start = 0;
            if (LOGGER.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }
            result = pjp.proceed();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("method invoke total cost [{}] ms", (System.currentTimeMillis() - start));
            }

            // catch return type for next time
            if (result != null) {
                ret.setType(result.getClass());
            }
        } else {
            if (ret.isCollection()) {
                result = ResultTranslateUtils.toCollection(returnType, keyValueMap.values());
            } else {
                result = ResultTranslateUtils.toMap(returnType, keyValueMap, keyIdMap);
            }
        }

        return result;
    }

    private Object[] toMissArgs(Set<String> missKeys, Map<String, Object> keyIdMap,
                                Object[] args, int batchIndex)
            throws
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {

        Collection<Object> missIds = new ArrayList<>(missKeys.size());
        for (String key : missKeys) {
            Object id = keyIdMap.get(key);
            missIds.add(id);
        }

        Object batchArg = args[batchIndex];
        Class<?> batchArgClass = batchArg.getClass();

        args[batchIndex] = batchArgClass.getConstructor(Collection.class).newInstance(missIds);

        return args;
    }

    private void doRecord(BatchReadResult batchReadResult, String keyPattern) {
        if (this.shootingMXBean != null) {
            Set<String> missKeys = batchReadResult.getMissKeys();

            int hitCount = batchReadResult.getHitKeyValueMap().size();
            int totalCount = hitCount + missKeys.size();

            this.shootingMXBean.hitIncr(keyPattern, hitCount);
            this.shootingMXBean.requireIncr(keyPattern, totalCount);

            LOGGER.info("multi cache hit shooting: {}/{}, missed keys: {}",
                    hitCount, totalCount, missKeys);
        }
    }
}
