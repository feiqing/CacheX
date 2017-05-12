package com.alibaba.cacher.support.serialize;

import com.alibaba.cacher.IObjectSerializer;
import com.alibaba.cacher.exception.CacherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author jifang
 * @since 2016/7/2 下午5:00.
 */
public class JdkSerializer implements IObjectSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdkSerializer.class);

    @Override
    public <T> byte[] serialize(T obj) {
        byte[] bytes = null;
        if (obj != null) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 GZIPOutputStream gzout = new GZIPOutputStream(bos);
                 ObjectOutputStream out = new ObjectOutputStream(gzout)) {

                out.writeObject(obj);
                bytes = bos.toByteArray();

            } catch (IOException e) {
                LOGGER.error("Jdk serialize error", e);
                throw new CacherException(e);
            }
        }

        return bytes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) {
        T obj = null;
        if (bytes != null) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                 GZIPInputStream gzin = new GZIPInputStream(bis);
                 ObjectInputStream ois = new ObjectInputStream(gzin)) {

                obj = (T) ois.readObject();

            } catch (Exception e) {
                LOGGER.error("Hessian deserialize error", e);
                throw new CacherException(e);
            }
        }

        return obj;
    }
}
