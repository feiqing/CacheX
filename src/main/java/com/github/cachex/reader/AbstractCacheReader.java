package com.github.cachex.reader;

import com.github.cachex.domain.CacheKeyHolder;
import com.github.cachex.domain.CacheMethodHolder;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.utils.CacheXLogger;

/**
 * @author jifang
 * @since 2016/11/5 下午3:22.
 */
public abstract class AbstractCacheReader {

    public abstract Object read(CacheKeyHolder cacheKeyHolder, CacheMethodHolder cacheMethodHolder, Invoker invoker, boolean needWrite) throws Throwable;

    Object doLogInvoke(ThrowableSupplier<Object> throwableSupplier) throws Throwable {
        long start = 0;
        if (CacheXLogger.CACHEX.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }

        Object result = throwableSupplier.get();

        if (CacheXLogger.CACHEX.isDebugEnabled()) {
            CacheXLogger.CACHEX.debug("method invoke total cost [{}] ms", (System.currentTimeMillis() - start));
        }

        return result;
    }

    @FunctionalInterface
    protected interface ThrowableSupplier<T> {
        T get() throws Throwable;
    }
}
