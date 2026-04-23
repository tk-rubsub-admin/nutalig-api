package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlUtil {

    public static final char ESCAPE_CHAR = '\\';
    public static final String PERCENT = "%";
    public static final String UNDERSCORE = "_";

    public static String escape(String input) {
        return input
                .replace(PERCENT, ESCAPE_CHAR + PERCENT)
                .replace(UNDERSCORE, ESCAPE_CHAR + UNDERSCORE);
    }

    public static String buildContainString(String value) {
        return PERCENT + value + PERCENT;
    }
}
