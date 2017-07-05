package com.alibaba.cacher.cases;

import com.alibaba.cacher.IObjectSerializer;
import com.alibaba.cacher.domain.User;
import com.alibaba.cacher.support.serialize.Hessian2Serializer;
import net.sf.cglib.beans.BeanGenerator;
import org.junit.Test;

import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author jifang.zjf
 * @since 2017/7/4 下午8:57.
 */
public class BreakdownPreventTester {


    private IObjectSerializer serializer = new Hessian2Serializer();

    private static class Seri implements Serializable {

    }

    public class HH {
    }

    private static class MemoryClassLoader extends ClassLoader {

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    @Test
    public void test() throws IllegalAccessException, InstantiationException, IOException {

        BeanGenerator generator = new BeanGenerator();
        generator.setSuperclass(User.class);
        generator.addProperty("generate", HH.class);
        Object o = generator.create();

        System.out.println(o.getClass().getName());
        System.out.println(o.getClass().getName().contains("$$BeanGeneratorByCGLIB$$"));

        byte[] serialize = serializer.serialize(o);

        User user2 = serializer.deserialize(serialize);
        System.out.println(user2);

        Field[] declaredFields = user2.getClass().getDeclaredFields();

        Arrays.stream(declaredFields).forEach((field -> {
            System.out.println(field.getName());
            System.out.println(field.getType());
        }));

        //System.out.println(Arrays.toString(declaredFields));

        //System.out.println(o);


        StandardJavaFileManager standardFileManager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);

//        StandardJavaFileManager stdManager = compiler.getStandardFileManager(null, null, null);
//        try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
//            JavaFileObject javaFileObject = manager.makeStringSource(fileName, source);
//            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject));
//            if (task.call()) {
//                results = manager.getClassBytes();
//            }
//        }

//        // 1. 创建Enhancer对象
//        Enhancer e = new Enhancer();
//        e.setSuperclass(User.class);
//        e.setStrategy(new DefaultGeneratorStrategy() {
//            protected ClassGenerator transform(ClassGenerator cg) {
//                return cg;
//            }
//        });
//
//        User user = (User) e.create();
//
//        //Object delegatorProxy = new CglibProxyFactory().createDelegatorProxy(Seri::new, new Class[]{Serializable.class, User.class});
//        //System.out.println(delegatorProxy);
//
//


    }
}
