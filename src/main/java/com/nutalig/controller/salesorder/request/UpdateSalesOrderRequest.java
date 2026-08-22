package com.nutalig.controller.salesorder.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateSalesOrderRequest {
    private LocalDate docDate;
    private LocalDate expireDate;
    private String coSaleId;
    private BigDecimal subTotal;
    private BigDecimal amount;
    private BigDecimal commission;
    private BigDecimal coSaleCommission;
    private BigDecimal discount;
    private BigDecimal freight;
    private Boolean isVat;
    private String shippingType;
    private Boolean requestCoa;
    private Boolean requestPo;
    private LocalDate paymentScheduleDate;
    private String quotationNo;
    private String remark;
    private String shipping;
    private List<UpdateSalesOrderDetailRequest> items;
}
