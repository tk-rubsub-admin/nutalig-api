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

    public class MessageTemplateCode {
        public static final String RFQ_TRACKING_STATUS_TH = "rfqTrackingStatusTh";
        public static final String RFQ_NOT_FOUND_TH = "rfqNotFoundTh";
        public static final String QUOTATION_NOT_FOUND_TH = "quotationNotFoundTh";
        public static final String DOWNLOAD_QUOTATION_TH = "downloadQuotationTh";
    }
}
