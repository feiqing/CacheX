package com.alibaba.cacher.supplier;

import net.sf.cglib.beans.BeanGenerator;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf
 * @since 2017/7/5 下午5:53.
 */
public class PreventObjectSupplier {

    private static final ConcurrentMap<Class<?>, Object> preventObjectMap = new ConcurrentHashMap<>();

    public static Object generatePreventObject() {
        /// return preventObjectMap.computeIfAbsent(type, CGLibGenerator::createPreventObject);
        return PreventObject.instance;
    }



    public static boolean isGeneratePreventObject(Object object) {
        // return CGLibGenerator.isGeneratePreventObject(object);

        return object instanceof PreventObject;
    }

    private static final class PreventObject implements Serializable {

        private static final long serialVersionUID = -1102811488039755703L;

        private static final PreventObject instance = new PreventObject();
    }

    private static class CGLibGenerator {

        private static Object createPreventObject(Class<?> type) {
            BeanGenerator generator = new BeanGenerator();
            generator.setSuperclass(type);
            generator.addProperty("generated", PreventObject.class);

            return generator.create();
        }

        private static boolean isGeneratePreventObject(Object object) {
            Class<?> clazz = object.getClass();

            if (clazz.getName().contains("$$BeanGeneratorByCGLIB$$")) {
                Field[] fields = clazz.getDeclaredFields();
                if (fields != null
                        && fields.length == 1
                        && fields[0].getName().equals("generated")
                        && fields[0].getType() == PreventObject.class) {
                    return true;
                }
            }

            return false;
        }
    }
}
