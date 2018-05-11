package com.github.cachex.reader;

import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.domain.CacheMethodHolder;
import com.github.cachex.invoker.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang
 * @since 2016/11/5 下午3:22.
 */
public abstract class AbstractCacheReader {

    protected static final Logger LOGGER = LoggerFactory.getLogger("com.alibaba.cachex");

    public abstract Object read(CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder, Invoker invoker, boolean needWrite) throws Throwable;

    protected Object doLogInvoke(Supplier<Object> supplier) throws Throwable {
        long start = 0;
        if (LOGGER.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }

        Object result = supplier.get();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("method invoke total cost [{}] ms", (System.currentTimeMillis() - start));
        }

        return result;
    }

    @FunctionalInterface
    protected interface Supplier<T> {
        T get() throws Throwable;
    }
}
