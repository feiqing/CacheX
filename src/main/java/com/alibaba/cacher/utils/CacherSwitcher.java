package com.alibaba.cacher.utils;

import com.alibaba.cacher.Invalid;
import com.google.common.base.Strings;
import com.alibaba.cacher.Cached;
import com.alibaba.cacher.enums.Expire;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 2017/1/9 下午4:59.
 */
public class CacherSwitcher {

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static final Map<Method, String[]> methodArgNameMap = new ConcurrentHashMap<>();

    public static boolean isSwitchOn(boolean openStat,
                                     Cached cached,
                                     Method method, Object[] args) {
        return isInnerSwitchOn(openStat, cached.expire(), cached.condition(), method, args);
    }

    public static boolean isSwitchOn(boolean openStat,
                                     Invalid invalid,
                                     Method method, Object[] args) {
        return isInnerSwitchOn(openStat, Expire.FOREVER, invalid.condition(), method, args);
    }

    private static boolean isInnerSwitchOn(boolean openStat,
                                           int expire,
                                           String condition, Method method, Object[] args) {

        if (openStat && expire != Expire.NO) {
            if (!Strings.isNullOrEmpty(condition)) {
                String[] argNames = getArgNames(method);

                return calcSpelCondition(condition, argNames, args);
            }

            return true;
        }

        return false;
    }


    private static String[] getArgNames(Method method) {
        String[] argNames = methodArgNameMap.get(method);
        if (argNames == null) {
            argNames = CacherUtils.getMethodVariableMap(method);

            methodArgNameMap.put(method, argNames);
        }

        return argNames;
    }

    private static boolean calcSpelCondition(String conditionSpel, String[] argNames, Object[] argValues) {
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < argValues.length; ++i) {
            String argName = argNames[i];
            Object argValue = argValues[i];

            context.setVariable(argName, argValue);
        }

        return parser.parseExpression(conditionSpel).getValue(context, boolean.class);
    }
}
