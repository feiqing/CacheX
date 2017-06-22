package com.alibaba.cacher.utils;

import com.alibaba.cacher.config.Inject;
import com.alibaba.cacher.config.Singleton;
import com.alibaba.cacher.exception.CacherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author jifang
 * @since 2016/10/27 上午9:38.
 */
@SuppressWarnings("unchecked")
public class CacherIOCContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger("com.alibaba.cacher");

    private static final String CLASS_SUFFIX = ".class";
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";

    private static ConcurrentMap<String, Object> nameBeanMap = new ConcurrentHashMap<>();

    private static ConcurrentMap<Class<?>, Object> classBeanMap = new ConcurrentHashMap<>();

    public static void init(Object rootBean, String... packageNames) throws IOException {
        Class<?> rootBeanClass = rootBean.getClass();
        Set<Class<?>> classes = new HashSet<>();
        for (String packageName : packageNames) {
            classes.addAll(packageScan(packageName));
        }

        savePackageBeanInstance(classes, rootBeanClass, rootBean);

        injectBeanField();
    }

    public static <T> T getBeanInstance(Class<T> clazz) {
        Object bean = classBeanMap.get(clazz);
        if (bean == null) {
            for (Object beanInstance : classBeanMap.values()) {
                if (clazz.isAssignableFrom(beanInstance.getClass())) {
                    bean = beanInstance;
                    break;
                }
            }
        }
        return (T) bean;
    }

    public static <T> T getBeanInstance(String beanId) {
        return (T) nameBeanMap.get(beanId);
    }

    public static void registerBeanInstance(Object bean) {
        Class<?> clazz = bean.getClass();
        classBeanMap.put(clazz, bean);
        nameBeanMap.put(getBeanId(clazz), bean);
    }

    private static void savePackageBeanInstance(Set<Class<?>> classes, Class<?> rootBeanClass, Object rootBean) {
        Map<String, Object> nameBeanMap = new HashMap<>(classes.size());
        Map<Class<?>, Object> classBeanMap = new HashMap<>(classes.size());

        for (Class<?> clazz : classes) {

            Object beanInstance = newBeanInstance(clazz, rootBeanClass, rootBean);
            String beanId = getBeanId(clazz);

            nameBeanMap.put(beanId, beanInstance);
            classBeanMap.put(clazz, beanInstance);
        }

        CacherIOCContainer.nameBeanMap.putAll(nameBeanMap);
        CacherIOCContainer.classBeanMap.putAll(classBeanMap);
    }

    private static Object newBeanInstance(Class<?> clazz, Class<?> rootBeanClass, Object rootBean) {
        Object beanInstance = null;
        if (clazz == rootBeanClass) {
            beanInstance = rootBean;
        } else {
            try {
                beanInstance = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                // no chance
            }
        }

        return beanInstance;
    }

    private static void injectBeanField() {
        for (Map.Entry<Class<?>, Object> entry : classBeanMap.entrySet()) {
            Object beanInstance = entry.getValue();

            Field[] fields = entry.getKey().getDeclaredFields();

            injectField(beanInstance, fields);
        }
    }

    private static void injectField(Object beanInstance, Field[] fields) {
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())
                    && !Modifier.isFinal(field.getModifiers())
                    && !Modifier.isVolatile(field.getModifiers())) {

                Inject inject = field.getAnnotation(Inject.class);
                if (inject != null) {
                    Class<?> filedType = field.getType();


                    Object bean = nameBeanMap.get(inject.qualifierName());
                    if (bean == null) {
                        bean = nameBeanMap.get(field.getName());
                    }
                    if (bean == null) {
                        bean = classBeanMap.get(inject.qualifierClass());
                    }
                    if (bean == null) {
                        bean = getBeanInstance(filedType);
                    }

                    if (bean == null) {
                        if (inject.optional()) {
                            LOGGER.warn("field {}.{} not injected, because no related bean instance",
                                    field.getDeclaringClass().getName(),
                                    field.getName());
                        } else {
                            throw new CacherException("field " + field.getDeclaringClass().getName()
                                    + "." + field.getName() + " inject error, because no related bean instance");
                        }
                    } else {
                        field.setAccessible(true);
                        try {
                            field.set(beanInstance, bean);
                        } catch (IllegalAccessException ignored) {
                            // no chance
                        }
                    }
                }
            }
        }
    }


    private static Set<Class<?>> packageScan(String packageName) throws IOException {
        Set<Class<?>> classes = new HashSet<>();

        String packageDir = packageName.replace('.', '/');
        Enumeration<URL> packageResources = Thread.currentThread().getContextClassLoader().getResources(packageDir);
        while (packageResources.hasMoreElements()) {
            URL packageResource = packageResources.nextElement();

            String protocol = packageResource.getProtocol();
            // in project class
            if (FILE_PROTOCOL.equals(protocol)) {
                String packageDirPath = URLDecoder.decode(packageResource.getPath(), "UTF-8");
                scanProjectPackage(packageName, packageDirPath, classes);
            }
            // jar package class
            else if (JAR_PROTOCOL.equals(protocol)) {
                JarFile jar = ((JarURLConnection) packageResource.openConnection()).getJarFile();
                scanJarPackage(packageDir, jar, classes);
            }
        }

        return classes;
    }

    private static void scanJarPackage(String packageDir, JarFile jar, Set<Class<?>> classes) throws IOException {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (entryName.startsWith(packageDir) && entryName.endsWith(CLASS_SUFFIX)) {
                String cassNameWithPackage = trimClassSuffix(entryName.replace('/', '.'));
                saveClass(cassNameWithPackage, classes);
            }
        }
    }

    private static void scanProjectPackage(String packageName, String packageDirPath, Set<Class<?>> classes) {

        File packageDirFile = new File(packageDirPath);
        if (packageDirFile.exists() && packageDirFile.isDirectory()) {

            File[] subFiles = packageDirFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() || pathname.getName().endsWith(CLASS_SUFFIX);
                }
            });

            assert subFiles != null;
            for (File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    String subPackageName = packageName + "." + subFile.getName();
                    String subPackageDirPath = subFile.getAbsolutePath();

                    // recursion
                    scanProjectPackage(subPackageName, subPackageDirPath, classes);
                } else {
                    String classNameWithPackage = packageName + "." + trimClassSuffix(subFile.getName());
                    saveClass(classNameWithPackage, classes);
                }
            }
        }
    }

    // with .class suffix
    private static String trimClassSuffix(String classNameWithSuffix) {
        int endIndex = classNameWithSuffix.length() - CLASS_SUFFIX.length();
        return classNameWithSuffix.substring(0, endIndex);
    }

    private static void saveClass(String cassNameWithPackage, Set<Class<?>> classes) {
        try {
            Class<?> clazz = Class.forName(cassNameWithPackage);
            if (clazz.isAnnotationPresent(Singleton.class)) {
                classes.add(clazz);
            }
        } catch (ClassNotFoundException ignored) {
            // no chance
        }
    }

    private static String getBeanId(Class<?> clazz) {
        String beanId = clazz.getSimpleName();
        return (beanId.substring(0, 1).toLowerCase() + beanId.substring(1));
    }
}