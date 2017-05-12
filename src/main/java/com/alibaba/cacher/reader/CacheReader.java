package com.alibaba.cacher.reader;

import com.alibaba.cacher.domain.MethodInfoHolder;
import com.alibaba.cacher.Cached;
import com.alibaba.cacher.domain.CacheKeyHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang
 * @since 2016/11/5 下午3:22.
 */
public interface CacheReader {

    Logger LOGGER = LoggerFactory.getLogger("com.alibaba.cacher");

    Object read(CacheKeyHolder rule, Cached cached, ProceedingJoinPoint pjp, MethodInfoHolder ret) throws Throwable;
}
