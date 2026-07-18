package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InvoiceDto {
    private String invoiceNo;
    private String salesOrderNo;
    private String quotationNo;
    private String docDate;
    private String dueDate;
    private InvoiceStatus status;
    private DocumentStatusProfileDto statusProfile;
    private Currency currency;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private EmployeeDto saleAccount;
    private String coSaleId;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal freight;
    private BigDecimal amount;
    private BigDecimal commission;
    private BigDecimal vatRate;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private BigDecimal paidTotal;
    private BigDecimal outstandingTotal;
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
    private List<InvoiceDetailDto> items;
    private List<InvoicePaymentDto> payments;
}
