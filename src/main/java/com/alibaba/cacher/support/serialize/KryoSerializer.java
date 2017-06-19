package com.alibaba.cacher.support.serialize;

import com.alibaba.cacher.IObjectSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author jifang.zjf
 * @since 2017/6/19 下午4:53.
 */
public class KryoSerializer implements IObjectSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializer.class);

    @Override
    public <T> byte[] serialize(T obj) {
        byte[] result = null;

        if (obj != null) {
            Kryo kryo = new Kryo();
            try {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     Output output = new Output(bos)) {
                    kryo.writeClassAndObject(output, obj);
                    output.flush();

                    result = bos.toByteArray();
                }
            } catch (IOException e) {
                LOGGER.error("kryo serialize error", e);
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) {
        Object result = null;
        if (bytes != null) {
            Kryo kryo = new Kryo();
            try (Input input = new Input(bytes)) {
                result = kryo.readClassAndObject(input);
            }
        }

        return (T) result;
    }
}
