package com.alibaba.cacher.utils;

import com.google.common.base.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * @author jifang
 * @since 16/7/19 下午4:59.
 */
public class CacherUtils {

    private static final SpelExpressionParser parser = new SpelExpressionParser();



    public static Method getMethod(JoinPoint pjp) throws NoSuchMethodException {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = pjp.getTarget().getClass().getDeclaredMethod(ms.getName(), method.getParameterTypes());
        }
        return method;
    }

    static Object getExpressionValue(String spEL, Object outerValue) {
        Object innerValue;
        if (!Strings.isNullOrEmpty(spEL)) {
            innerValue = parser.parseExpression(spEL).getValue(outerValue);
        } else {
            innerValue = outerValue;
        }
        return innerValue;
    }

}
