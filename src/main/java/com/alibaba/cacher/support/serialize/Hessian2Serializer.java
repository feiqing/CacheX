package com.alibaba.cacher.support.serialize;

import com.alibaba.cacher.exception.CacherException;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.alibaba.cacher.IObjectSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author zhoupan@weidian.com
 * @since 16/7/8.
 */
public class Hessian2Serializer implements IObjectSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hessian2Serializer.class);

    @Override
    public <T> byte[] serialize(T obj) {
        if (obj != null) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

                Hessian2Output out = new Hessian2Output(os);
                out.writeObject(obj);
                out.close();
                return os.toByteArray();

            } catch (IOException e) {
                LOGGER.error("Hessian serialize error ", e);
                throw new CacherException(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] bytes) {
        if (bytes != null) {
            try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {

                Hessian2Input in = new Hessian2Input(is);
                T obj = (T) in.readObject();
                in.close();

                return obj;

            } catch (IOException e) {
                LOGGER.error("Hessian deserialize error ", e);
                throw new CacherException(e);
            }
        }
        return null;
    }
}
