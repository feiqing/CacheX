package com.github.cachex.exception;

/**
 * @author jifang
 * @since 16/7/18 下午4:07.
 */
public class CacheXException extends RuntimeException {

    private static final long serialVersionUID = 6582898095150572577L;

    public CacheXException(String message) {
        super(message);
    }

    public CacheXException(Throwable cause) {
        super(cause);
    }

    public CacheXException(String message, Throwable cause) {
        super(message, cause);
    }
}
