package com.github.cachex.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-28 18:51:00.
 */
public class CacheXLogger {

    private static Logger logger = LoggerFactory.getLogger("com.github.cachex");

    public static void debug(String format, Object... arguments) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arguments);
        }
    }

    public static void info(String format, Object... arguments) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arguments);
        }
    }

    public static void warn(String format, Object... arguments) {
        if (logger.isWarnEnabled()) {
            logger.warn(format, arguments);
        }
    }

    public static void error(String format, Object... arguments) {
        logger.error(format, arguments);
    }
}
