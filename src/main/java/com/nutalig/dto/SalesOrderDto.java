package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.SalesOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesOrderDto {
    private String salesOrderNo;
    private String docDate;
    private String expireDate;
    private SalesOrderStatus status;
    private Currency currency;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private EmployeeDto saleAccount;
    private String coSaleId;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal freight;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private BigDecimal amount;
    private BigDecimal commission;
    private String shippingType;
    private BigDecimal vatRate;
    private String remark;
    private UserDto createdBy;
    private UserDto updatedBy;
    private Integer revNo;
    private List<SalesOrderDetailDto> items;
}
