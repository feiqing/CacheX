package com.alibaba.cacher.supplier;

import com.alibaba.cacher.exception.CacherException;
import com.alibaba.cacher.utils.SwitcherUtils;
import javassist.*;
import javassist.bytecode.LocalVariableAttribute;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf
 * @since 2017/6/23 上午10:17.
 */
public class ParameterNamesSupplier {

    private static final ConcurrentMap<Method, String[]> methodParameterNamesMap = new ConcurrentHashMap<>();

    public static String[] getParameterNames(Method method) {
        return methodParameterNamesMap.computeIfAbsent(method, ParameterNamesSupplier::doGetParameterNames);
    }

    private static String[] doGetParameterNames(Method method) {

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
        Collection<String> parameterNames = variableMap.values();
        int offset = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;

        return Arrays.copyOfRange(parameterNames.toArray(), offset, offset + method.getParameterCount(), String[].class);
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
        pool.insertClassPath(new ClassClassPath(SwitcherUtils.class));
    }
}
