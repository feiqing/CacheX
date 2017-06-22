package com.alibaba.cacher.utils;

import com.alibaba.cacher.Cached;
import com.alibaba.cacher.CachedGet;
import com.alibaba.cacher.Invalid;
import com.alibaba.cacher.enums.Expire;
import com.alibaba.cacher.exception.CacherException;
import com.google.common.base.Strings;
import javassist.*;
import javassist.bytecode.LocalVariableAttribute;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 2017/1/9 下午4:59.
 */
public class CacherSwitcher {

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static final Map<Method, String[]> methodArgNameMap = new ConcurrentHashMap<>();

    public static boolean isSwitchOn(boolean openStat, Cached cached, Method method, Object[] args) {
        return doIsSwitchOn(openStat, cached.expire(), cached.condition(), method, args);
    }

    public static boolean isSwitchOn(boolean openStat, Invalid invalid, Method method, Object[] args) {
        return doIsSwitchOn(openStat, Expire.FOREVER, invalid.condition(), method, args);
    }

    public static boolean isSwitchOn(boolean openStat, CachedGet cachedGet, Method method, Object[] args) {
        return doIsSwitchOn(openStat, Expire.FOREVER, cachedGet.condition(), method, args);
    }

    private static boolean doIsSwitchOn(boolean openStat,
                                        int expire,
                                        String condition, Method method, Object[] args) {

        if (openStat && expire != Expire.NO) {
            if (!Strings.isNullOrEmpty(condition)) {
                String[] argNames = methodArgNameMap.computeIfAbsent(method, CacherSwitcher::getParameterNames);

                return calcSpelCondition(condition, argNames, args);
            }

            return true;
        }

        return false;
    }

    private static boolean calcSpelCondition(String conditionSpel, String[] argNames, Object[] argValues) {
        // 将[参数名->参数值]导入spel环境
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < argValues.length; ++i) {
            String argName = argNames[i];
            Object argValue = argValues[i];

            context.setVariable(argName, argValue);
        }

        // 计算
        return parser.parseExpression(conditionSpel).getValue(context, boolean.class);
    }

    private static String[] getParameterNames(Method method) {

        CtMethod ctMethod = getCtMethod(method);

        // 1. 拿到方法局部变量表
        LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) ctMethod
                .getMethodInfo2()
                .getCodeAttribute()
                .getAttribute(LocalVariableAttribute.tag);

        // 2. 组织为有序map(以变量在表中所处的slots位置排序)
        // 详见: http://www.cnblogs.com/hucn/p/3636912.html
        SortedMap<Integer, String> variableMap = new TreeMap<>();
        for (int i = 0; i < localVariableAttribute.tableLength(); i++) {
            variableMap.put(localVariableAttribute.index(i), localVariableAttribute.variableName(i));
        }

        // 3. 将有序的参数名列表导出到array
        String[] names;
        Collection<String> parameterNames = variableMap.values();
        if (Modifier.isStatic(ctMethod.getModifiers())) {
            names = parameterNames.toArray(new String[parameterNames.size()]);
        } else {
            names = Arrays.copyOfRange(parameterNames.toArray(), 1, parameterNames.size(), String[].class);
        }

        return names;
    }

    private static CtMethod getCtMethod(Method method) {

        try {
            // 方法所属的ctClass
            CtClass methodClass = getCtClass(method.getDeclaringClass());

            // 方法参数所属的ctClass
            Class<?>[] parameterTypes = method.getParameterTypes();
            CtClass[] parameterClass = new CtClass[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; ++i) {
                parameterClass[i] = getCtClass(parameterTypes[i]);
            }

            return methodClass.getDeclaredMethod(method.getName(), parameterClass);
        } catch (NotFoundException e) {
            throw new CacherException(e);
        }
    }

    private static CtClass getCtClass(Class<?> clazz) throws NotFoundException {
        return pool.getCtClass(clazz.getName());
    }

    private static final ClassPool pool;

    static {
        pool = ClassPool.getDefault();
        // 自定义ClassLoader情况
        pool.insertClassPath(new ClassClassPath(CacherSwitcher.class));
    }
}
