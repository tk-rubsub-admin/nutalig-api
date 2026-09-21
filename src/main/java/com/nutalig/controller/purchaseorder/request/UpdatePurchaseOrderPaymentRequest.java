package com.nutalig.controller.purchaseorder.request;

import com.nutalig.constant.PaymentMethod;
import com.nutalig.constant.PurchaseOrderPaymentType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class UpdatePurchaseOrderPaymentRequest {
    private Long scheduleId;
    private PurchaseOrderPaymentType paymentType;
    private Integer installmentNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime paymentDate;

    private BigDecimal amount;
    private BigDecimal exchangeRate;
    private PaymentMethod paymentMethod;
    private String transferReference;
    private String chequeBank;
    private String chequeNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate chequeDate;

    private String chequeBranch;
    private String remark;
}
