package com.github.cachex.utils;

import com.github.cachex.CacheKey;
import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.supplier.ParameterNamesSupplier;
import com.github.cachex.supplier.SpelValueSupplier;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.cachex.utils.CacheXUtils.appendSeparator;

/**
 * @author jifang
 * @since 16/7/21 上午11:34.
 */
public class KeyGenerators {

    public static String generateSingleKey(CacheKeyHolder cacheKeyHolder, Object[] args) {
        String[] parameterNames = ParameterNamesSupplier.getParameterNames(cacheKeyHolder.getMethod());
        StringBuilder sb = new StringBuilder(cacheKeyHolder.getPrefix());
        // -> "keyExp"

        cacheKeyHolder.getCacheKeyMap().forEach((index, cacheKey) -> {
            // append key separator (like : "-")
            appendSeparator(sb, cacheKeyHolder.getPrefix(), index, cacheKeyHolder.getSeparator());
            // -> "keyExp-"

            // append key keyExp (like: "id:")
            sb.append(cacheKey.prefix());
            // -> "keyExp-id:"

            // append argValue (like: "[id-value]、[name-value]")
            Object argValue = SpelValueSupplier.calcSpelValue(cacheKey.spel(), parameterNames, args, args[index]);
            sb.append(argValue);
            // -> "keyExp-id:101791"
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
        String separator = cacheKeyHolder.getSeparator();
        Map<Integer, CacheKey> cacheKeyMap = cacheKeyHolder.getCacheKeyMap();
        String[] parameterNames = (String[]) appendArray(
                ParameterNamesSupplier.getParameterNames(cacheKeyHolder.getMethod()),
                "index");
        Object multiArg = args[cacheKeyHolder.getMultiIndex()];

        // -- 开始拼装 -- //
        if (multiArg != null) {
            Collection multiArgEntries = multiArg instanceof Collection ? (Collection) multiArg : ((Map) multiArg).keySet();     // 被标记为multi的参数值

            int multiArgEntryIndex = 0;
            for (Object multiArgEntry : multiArgEntries) {
                String key = doGenerateKey(multiIndex, prefix, separator, cacheKeyMap,
                        parameterNames, args,
                        multiArgEntry, multiArgEntryIndex++);

                key2Id.put(key, multiArgEntry);
                id2Key.put(multiArgEntry, key);
            }
        }

        return new Map[]{id2Key, key2Id};
    }

    private static String doGenerateKey(int multiIndex, String prefix, String separator, Map<Integer, CacheKey> cacheKeyMap,
                                        String[] parameterNames, Object[] parameterValues,
                                        Object multiArgEntry, int multiArgEntryIndex) {
        StringBuilder sb = new StringBuilder(prefix);
        // -> "keyExp"

        // 将被spel计算出来的value值作为与result一对一的id
        for (Map.Entry<Integer, CacheKey> entry : cacheKeyMap.entrySet()) {
            int parameterIndex = entry.getKey();
            CacheKey cacheKey = entry.getValue();

            // append key separator (like : "-")
            appendSeparator(sb, prefix, parameterIndex, separator);
            // -> "keyExp-"

            // append key keyExp (like: "id:")
            sb.append(cacheKey.prefix());
            // -> "keyExp-id:"

            // append argValue (like: "[id-value]、[name-value]")
            Object argEntryValue = SpelValueSupplier.calcSpelValue(cacheKey.spel(),
                    parameterNames, () -> appendArray(parameterValues, multiArgEntryIndex),
                    parameterIndex == multiIndex ? multiArgEntry : parameterValues[parameterIndex]);

            sb.append(argEntryValue);
            // -> "keyExp-id:101791"
        }

        return sb.toString();
    }

    private static Object[] appendArray(Object[] origin, Object append) {
        Object[] dest = Arrays.copyOf(origin, origin.length + 1);
        dest[origin.length] = append;

        return dest;
    }
}
