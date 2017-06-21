package com.alibaba.cacher.utils;

import com.alibaba.cacher.exception.CacherException;
import com.google.common.base.Strings;
import javassist.*;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author jifang
 * @since 16/7/19 下午4:59.
 */
public class CacherUtils {

    private static final SpelExpressionParser parser = new SpelExpressionParser();

    private static final ClassPool pool;

    static {
        pool = ClassPool.getDefault();
        // 考虑自定义自定义ClassLoader的情况
        pool.insertClassPath(new ClassClassPath(CacherUtils.class));
    }

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

    static String[] getMethodVariableMap(Method method) {

        CtMethod ctMethod = getCtMethod(method);

        // 拿到Class文件内局部变量表
        MethodInfo methodInfo = ctMethod.getMethodInfo2();
        LocalVariableAttribute attribute = (LocalVariableAttribute) methodInfo.getCodeAttribute()
                .getAttribute(LocalVariableAttribute.tag);

        // 组织为有序Map
        SortedMap<Integer, String> variableMap = new TreeMap<>();
        for (int i = 0; i < attribute.tableLength(); i++) {
            variableMap.put(attribute.index(i), attribute.variableName(i));
        }

        Collection<String> values = variableMap.values();
        String[] names;

        if (Modifier.isStatic(ctMethod.getModifiers())) {
            names = values.toArray(new String[values.size()]);
        } else {
            names = Arrays.copyOfRange(values.toArray(), 1, values.size(), String[].class);
        }

        return names;
    }

    private static CtMethod getCtMethod(Method method) {

        try {
            // method ct class
            CtClass ctClazz = getCtClass(method.getDeclaringClass());

            // method param ct classes
            Class<?>[] pTypes = method.getParameterTypes();
            CtClass[] pCtClasses = new CtClass[pTypes.length];
            for (int i = 0; i < pTypes.length; ++i) {
                pCtClasses[i] = getCtClass(pTypes[i]);
            }

            return ctClazz.getDeclaredMethod(method.getName(), pCtClasses);
        } catch (NotFoundException e) {
            throw new CacherException(e);
        }
    }


    static StringBuilder appendSeparator(StringBuilder sb, String separator, String totalPrefix, int index) {
        // append key separator (like : "-")
        if (!Strings.isNullOrEmpty(separator)) {
            if (!Strings.isNullOrEmpty(totalPrefix) || index != 0) {
                sb.append(separator);
            }
        }

        return sb;
    }

    private static CtClass getCtClass(Class<?> clazz) throws NotFoundException {
        return pool.getCtClass(clazz.getName());
    }
}
