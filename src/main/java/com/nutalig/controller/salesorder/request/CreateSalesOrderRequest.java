package com.nutalig.controller.salesorder.request;

import com.nutalig.constant.SalesOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSalesOrderRequest {
    private String rfqId;
    private SalesOrderStatus status;
    private LocalDate docDate;
    private LocalDate expireDate;
    private String customerId;
    private String customerAddressId;
    private String customerContactId;
    private String salesId;
    private String coSaleId;
    private BigDecimal coSaleCommission;
    private BigDecimal discount;
    private BigDecimal freight;
    private Boolean isVat;
    private String shippingType;
    private Boolean requestCoa;
    private Boolean requestPo;
    private String remark;
    private String shipping;
    private String quotationNo;
    private List<CreateSalesOrderDetailRequest> items;
}
