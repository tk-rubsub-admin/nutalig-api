package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {

    public static String maskText(String text) {
        if (text == null) {
            return null;
        }
        return text.substring(0, text.length() / 2) + "xxxx";
    }
}
