package com.nutalig.dto;

import com.nutalig.constant.InvoicePaymentStatus;
import com.nutalig.constant.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class InvoicePaymentDto {
    private Long id;
    private ZonedDateTime paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String chequeBank;
    private String chequeNo;
    private LocalDate chequeDate;
    private String chequeBranch;
    private String slipFileName;
    private String slipFileUrl;
    private List<InvoicePaymentSlipFileDto> slipFiles;
    private String receiptNo;
    private InvoicePaymentStatus status;
    private UserDto createdBy;
    private ZonedDateTime createdDate;
    private UserDto updatedBy;
    private ZonedDateTime updatedDate;
}
