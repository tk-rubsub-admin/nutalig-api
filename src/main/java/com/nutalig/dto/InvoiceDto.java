package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.InvoiceStatus;
import com.nutalig.constant.UrgentRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.time.ZonedDateTime;

@Data
public class InvoiceDto {
    private String invoiceNo;
    private String salesOrderNo;
    private String quotationNo;
    private String docDate;
    private String dueDate;
    private String deliveryDate;
    private InvoiceStatus status;
    private DocumentStatusProfileDto statusProfile;
    private Currency currency;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private QuotationCustomerSnapshotDto customerSnapshot;
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
    private String customerBranchCodeSnapshot;
    private String customerBranchNameSnapshot;
    private String customerAddressSnapshot;
    private String customerContactSnapshot;
    private String customerPhoneSnapshot;
    private SystemConfigDto customerPaymentTerm;
    private String salesNameSnapshot;
    private Boolean requiredApprove;
    private String requiredApproveReason;
    private UrgentRequestStatus requiredApproveStatus;
    private String requestRequiredApprovedBy;
    private ZonedDateTime requestRequiredApprovedDate;
    private String approvedBy;
    private ZonedDateTime approvedDate;
    private String rejectedBy;
    private ZonedDateTime rejectedDate;
    private String rejectReason;
    private UserDto createdBy;
    private UserDto updatedBy;
    private List<InvoiceDetailDto> items;
    private List<InvoicePaymentDto> payments;
}
