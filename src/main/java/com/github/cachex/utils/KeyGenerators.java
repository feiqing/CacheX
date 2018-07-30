package com.github.cachex.utils;

import com.github.cachex.CacheKey;
import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.supplier.ArgNameSupplier;
import com.github.cachex.supplier.SpelValueSupplier;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 16/7/21 上午11:34.
 */
public class KeyGenerators {

    public static String generateSingleKey(CacheKeyHolder cacheKeyHolder, Object[] args) {
        String[] parameterNames = ArgNameSupplier.getArgNames(cacheKeyHolder.getMethod());
        StringBuilder sb = new StringBuilder(cacheKeyHolder.getPrefix());
        cacheKeyHolder.getCacheKeyMap().forEach((index, cacheKey) -> {
            Object argValue = SpelValueSupplier.calcSpelValue(cacheKey.value(), parameterNames, args, args[index]);
            sb.append(argValue);
        });

        return sb.toString();
    }

    //array[]: {id2Key, key2Id}
    public static Map[] generateMultiKey(CacheKeyHolder cacheKeyHolder, Object[] args) {
        Map<Object/*这里就要求Multi-Collection内的元素必须实现的hashcode & equals方法*/, String> id2Key = new LinkedHashMap<>();
        Map<String, Object> key2Id = new LinkedHashMap<>();

        // -- 准备要拼装key所需的原材料 -- //
        int multiIndex = cacheKeyHolder.getMultiIndex();
        String prefix = cacheKeyHolder.getPrefix();
        Map<Integer, CacheKey> cacheKeyMap = cacheKeyHolder.getCacheKeyMap();
        String[] parameterNames = (String[]) appendArray(ArgNameSupplier.getArgNames(cacheKeyHolder.getMethod()), "forEachIndex");
        Object multiArg = args[cacheKeyHolder.getMultiIndex()];

        // -- 开始拼装 -- //
        if (multiArg != null) {
            Collection multiElements = multiArg instanceof Collection ? (Collection) multiArg : ((Map) multiArg).keySet();     // 被标记为multi的参数值

            int forEachIndex = 0;
            for (Object multiElement : multiElements) {
                String key = doGenerateMultiKey(prefix,
                        multiIndex, forEachIndex,
                        cacheKeyMap,
                        parameterNames, args, multiElement);

                key2Id.put(key, multiElement);
                id2Key.put(multiElement, key);
                ++forEachIndex;
            }
        }

        return new Map[]{id2Key, key2Id};
    }

    private static String doGenerateMultiKey(String prefix,
                                             int multiIndex, int forEachIndex,
                                             Map<Integer, CacheKey> index2Key,
                                             String[] argNames, Object[] argValues,
                                             Object multiArgElement) {

        StringBuilder sb = new StringBuilder(prefix);
        index2Key.forEach((argIndex, argCacheKey) -> {
            Object defaultValue = (argIndex != multiIndex ? argValues[argIndex] : multiArgElement);
            Object argEntryValue = SpelValueSupplier.calcSpelValue(argCacheKey.value(),
                    argNames, () -> appendArray(argValues, forEachIndex),
                    defaultValue);

            sb.append(argEntryValue);
        });

        return sb.toString();
    }

    private static Object[] appendArray(Object[] origin, Object append) {
        Object[] dest = Arrays.copyOf(origin, origin.length + 1);
        dest[origin.length] = append;

        return dest;
    }
}
