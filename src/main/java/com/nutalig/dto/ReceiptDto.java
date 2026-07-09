package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PaymentMethod;
import com.nutalig.constant.ReceiptStatus;
import com.nutalig.constant.ReceiptType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class ReceiptDto {
    private String receiptNo;
    private ReceiptType receiptType;
    private ReceiptStatus status;
    private String invoiceNo;
    private Long invoicePaymentId;
    private String salesOrderNo;
    private String quotationNo;
    private String docDate;
    private ZonedDateTime paidDate;
    private Currency currency;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private EmployeeDto saleAccount;
    private String coSaleId;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal amount;
    private BigDecimal vatRate;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private PaymentMethod paymentMethod;
    private String chequeBank;
    private String chequeNo;
    private String chequeDate;
    private String chequeBranch;
    private String slipFileName;
    private String slipFileUrl;
    private String remark;
    private Integer revNo;
    private String customerNameSnapshot;
    private String customerTaxIdSnapshot;
    private String customerAddressSnapshot;
    private String customerContactSnapshot;
    private String customerPhoneSnapshot;
    private String salesNameSnapshot;
    private UserDto createdBy;
    private UserDto updatedBy;
    private List<ReceiptDetailDto> items;
}
