package com.nutalig.controller.salesorder.request;

import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.constant.UrgentRequestStatus;
import com.nutalig.constant.ProcurementStatus;
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
    private UrgentRequestStatus urgentRequestStatus;
    private List<ProcurementStatus> procurementStatus;
    private String keyword;
}
