package com.alibaba.cacher.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jifang.zjf
 * @since 2017/7/12 上午10:56.
 */
public class StringFormatter {

    private static final Pattern pattern = Pattern.compile("\\$\\{(\\w)+}");

    public static String format(String template, Map<String, Object> argMap) {
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String exp = matcher.group();
            Object value = argMap.get(trim(exp));
            String expStrValue = getStringValue(value);

            template = template.replace(exp, expStrValue);
        }

        return template;
    }

    public static String format(String template, Object... args) {
        Matcher matcher = pattern.matcher(template);

        int index = 0;
        while (matcher.find()) {
            String exp = matcher.group();
            Object value = args[index++];
            String expStrValue = getStringValue(value);

            template = template.replace(exp, expStrValue);
        }

        return template;
    }

    private static String getStringValue(Object obj) {
        String string;

        if (obj instanceof String) {
            string = (String) obj;
        } else {
            string = String.valueOf(obj);
        }

        return string;
    }

    private static String trim(String string) {
        if (string.startsWith("${"))
            string = string.substring("${".length());

        if (string.endsWith("}"))
            string = string.substring(0, string.length() - "}".length());

        return string;
    }


    public static void main(String[] args) {
        String template = "jdbc:mysql://${host}:${port}/${database}";
        System.out.println(format(template, "127.0.0.1", 3306, "testdb"));
        System.out.println(format(template, "127.0.0.1", 3306, "testdb"));
    }
}
