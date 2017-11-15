package com.alibaba.cacher.utils;

import com.google.common.base.Strings;

/**
 * @author jifang
 * @since 16/7/19 下午4:59.
 */
public class CacherUtils {

    public static StringBuilder appendSeparator(StringBuilder sb, String prefix, int index, String separator) {
        if (!Strings.isNullOrEmpty(separator)) {
            if (!Strings.isNullOrEmpty(prefix) || index != 0) {
                sb.append(separator);
            }
        }

        return sb;
    }
}
