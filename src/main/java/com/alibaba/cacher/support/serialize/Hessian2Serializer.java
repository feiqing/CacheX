package com.alibaba.cacher.support.serialize;

import com.alibaba.cacher.IObjectSerializer;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
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
        byte[] result = null;

        if (obj != null) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

                Hessian2Output out = new Hessian2Output(os);
                out.writeObject(obj);
                out.close();
                result = os.toByteArray();
            } catch (IOException e) {
                LOGGER.error("Hessian serialize error ", e);
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) {
        Object result = null;
        if (bytes != null) {
            try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
                Hessian2Input in = new Hessian2Input(is);
                result = in.readObject();
                in.close();
            } catch (IOException e) {
                LOGGER.error("Hessian deserialize error ", e);
            }
        }

        return (T) result;
    }
}
