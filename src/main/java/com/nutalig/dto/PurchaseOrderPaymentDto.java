package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PaymentMethod;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.constant.PurchaseOrderPaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class PurchaseOrderPaymentDto {
    private Long id;
    private Long scheduleId;
    private PurchaseOrderPaymentType paymentType;
    private Integer installmentNo;
    private ZonedDateTime paymentDate;
    private BigDecimal amount;
    private Currency currency;
    private BigDecimal exchangeRate;
    private BigDecimal amountThb;
    private PaymentMethod paymentMethod;
    private String transferReference;
    private String chequeBank;
    private String chequeNo;
    private LocalDate chequeDate;
    private String chequeBranch;
    private String remark;
    private PurchaseOrderPaymentStatus status;
    private String rejectionReason;
    private UserDto approvedBy;
    private ZonedDateTime approvedDate;
    private UserDto rejectedBy;
    private ZonedDateTime rejectedDate;
    private UserDto voidedBy;
    private ZonedDateTime voidedDate;
    private String voidReason;
    private String requestKey;
    private UserDto createdBy;
    private ZonedDateTime createdDate;
    private UserDto updatedBy;
    private ZonedDateTime updatedDate;
    private List<PurchaseOrderPaymentAttachmentDto> attachments;
}
