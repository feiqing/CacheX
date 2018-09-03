package com.github.cachex.reader;

import com.github.cachex.domain.CacheXAnnoHolder;
import com.github.cachex.domain.CacheXMethodHolder;
import com.github.cachex.invoker.Invoker;
import com.github.cachex.utils.CacheXLogger;

/**
 * @author jifang
 * @since 2016/11/5 下午3:22.
 */
public abstract class AbstractCacheReader {

    public abstract Object read(CacheXAnnoHolder cacheXAnnoHolder, CacheXMethodHolder cacheXMethodHolder, Invoker invoker, boolean needWrite) throws Throwable;

    Object doLogInvoke(ThrowableSupplier<Object> throwableSupplier) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return throwableSupplier.get();
        } finally {
            CacheXLogger.debug("method invoke total cost [{}] ms", (System.currentTimeMillis() - start));
        }
    }

    @FunctionalInterface
    protected interface ThrowableSupplier<T> {
        T get() throws Throwable;
    }
}
