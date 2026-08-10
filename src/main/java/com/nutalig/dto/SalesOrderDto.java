package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.ProcurementStatus;
import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.constant.SalesOrderPaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesOrderDto {
    private String salesOrderNo;
    private String docDate;
    private String expireDate;
    private SalesOrderStatus status;
    private DocumentStatusProfileDto statusProfile;
    private Currency currency;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private EmployeeDto saleAccount;
    private String rfqId;
    private String coSaleId;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal freight;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private BigDecimal amount;
    private SalesOrderPaymentStatus paymentStatus;
    private BigDecimal paidTotal;
    private BigDecimal outstandingTotal;
    private BigDecimal commission;
    private BigDecimal coSaleCommission;
    private Boolean requestCoa;
    private Boolean requestPo;
    private ProcurementStatus procurementStatus;
    private String shippingType;
    private BigDecimal vatRate;
    private String remark;
    private UserDto createdBy;
    private UserDto updatedBy;
    private Integer revNo;
    private List<SalesOrderAttachmentDto> attachments;
    private List<SalesOrderDetailDto> items;
}
