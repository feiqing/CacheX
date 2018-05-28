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
            sb.append(cacheKey.prefix());
            Object argValue = SpelValueSupplier.calcSpelValue(cacheKey.spel(), parameterNames, args, args[index]);
            sb.append(argValue);
        });

        return sb.toString();
    }


    //array[]: {id2Key, key2Id}
    public static Map[] generateMultiKey(CacheKeyHolder cacheKeyHolder, Object[] args) {
        Map<Object, String> id2Key = new LinkedHashMap<>();
        Map<String, Object> key2Id = new LinkedHashMap<>();

        // -- 准备要拼装key所需的原材料 -- //
        int multiIndex = cacheKeyHolder.getMultiIndex();
        String prefix = cacheKeyHolder.getPrefix();
        Map<Integer, CacheKey> cacheKeyMap = cacheKeyHolder.getCacheKeyMap();
        String[] parameterNames = (String[]) appendArray(ArgNameSupplier.getArgNames(cacheKeyHolder.getMethod()), "index");
        Object multiArg = args[cacheKeyHolder.getMultiIndex()];

        // -- 开始拼装 -- //
        if (multiArg != null) {
            Collection multiArgEntries = multiArg instanceof Collection ? (Collection) multiArg : ((Map) multiArg).keySet();     // 被标记为multi的参数值

            int multiArgEntryIndex = 0;
            for (Object multiArgEntry : multiArgEntries) {
                String key = doGenerateKey(multiIndex, prefix, cacheKeyMap,
                        parameterNames, args,
                        multiArgEntry, multiArgEntryIndex++);

                key2Id.put(key, multiArgEntry);
                id2Key.put(multiArgEntry, key);
            }
        }

        return new Map[]{id2Key, key2Id};
    }

    private static String doGenerateKey(int multiIndex, String prefix, Map<Integer, CacheKey> cacheKeyMap,
                                        String[] parameterNames, Object[] parameterValues,
                                        Object multiArgEntry, int multiArgEntryIndex) {
        StringBuilder sb = new StringBuilder(prefix);


        for (Map.Entry<Integer, CacheKey> entry : cacheKeyMap.entrySet()) {
            int parameterIndex = entry.getKey();
            CacheKey cacheKey = entry.getValue();
            sb.append(cacheKey.prefix());

            Object argEntryValue = SpelValueSupplier.calcSpelValue(cacheKey.spel(),
                    parameterNames, () -> appendArray(parameterValues, multiArgEntryIndex),
                    parameterIndex == multiIndex ? multiArgEntry : parameterValues[parameterIndex]);

            sb.append(argEntryValue);
        }

        return sb.toString();
    }

    private static Object[] appendArray(Object[] origin, Object append) {
        Object[] dest = Arrays.copyOf(origin, origin.length + 1);
        dest[origin.length] = append;

        return dest;
    }
}
