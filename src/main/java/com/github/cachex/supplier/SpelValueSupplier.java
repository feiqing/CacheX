package com.github.cachex.supplier;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.function.Supplier;

/**
 * Spel表达式的计算功能(@Cached内的condition、@CacheKey内的spel只是作为一个增值服务, 并不作为核心功能, 只是作为key拼装的一个亮点, 并不是必须功能)
 *
 * @author jifang.zjf
 * @since 2017/6/23 上午10:10.
 */
public class SpelValueSupplier {

    private static final ExpressionParser parser = new SpelExpressionParser();

    public static Object calcSpelWithLazyKey(String spel, Supplier<String[]> keysSupplier, Object[] values, Object defaultValue) {
        return calcSpelValue(spel, keysSupplier, () -> values, defaultValue);
    }

    public static Object calcSpelValue(String spel, String[] keys, Supplier<Object[]> valuesSupplier, Object defaultValue) {
        return calcSpelValue(spel, () -> keys, valuesSupplier, defaultValue);
    }

    public static Object calcSpelValue(String spel, String[] keys, Object[] values, Object defaultValue) {
        return calcSpelWithLazyKey(spel, () -> keys, values, defaultValue);
    }

    // 利用Supplier延迟计算特性, 防止不必要的参数名获取/参数值计算
    private static Object calcSpelValue(String spel, Supplier<String[]> keysSupplier, Supplier<Object[]> valuesSupplier, Object defaultValue) {
        if (Strings.isNullOrEmpty(spel)) {
            return defaultValue;
        }

        // 将[参数名->参数值]导入spel环境
        EvaluationContext context = new StandardEvaluationContext();

        String[] argNames = keysSupplier.get();
        Object[] argValues = valuesSupplier.get();
        Preconditions.checkState(argNames.length == argValues.length);
        for (int i = 0; i < argValues.length; ++i) {
            context.setVariable(argNames[i], argValues[i]);
        }

        return parser.parseExpression(spel).getValue(context);
    }

    public static Object calcSpelWithNoContext(String spel, Object defaultValue) {
        if (Strings.isNullOrEmpty(spel)) {
            return defaultValue;
        }

        return parser.parseExpression(spel).getValue(defaultValue);
    }
}
