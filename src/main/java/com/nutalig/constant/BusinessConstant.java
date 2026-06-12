package com.nutalig.constant;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class BusinessConstant {
    public static final BigDecimal VAT_RATE = new BigDecimal("0.07");

    public class DocumentPrefix {
        public static final String SALES_ORDER_PREFIX = "NTL-SO";
        public static final String QUOTATION_PREFIX = "NTL-QT";
    }
}
