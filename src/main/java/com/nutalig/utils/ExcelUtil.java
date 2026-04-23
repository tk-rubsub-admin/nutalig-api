package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelUtil {

    public static double toDoubleOrZero(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    public static int toIntOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
