package com.nutalig.controller.salesorder.request;

import com.nutalig.constant.SalesOrderStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SearchSalesOrderRequest {
    private String salesOrderNo;
    private LocalDate docDateStart;
    private LocalDate docDateEnd;
    private String customerId;
    private String salesId;
    private SalesOrderStatus status;
    private List<SalesOrderStatus> statuses;
    private String keyword;
}
