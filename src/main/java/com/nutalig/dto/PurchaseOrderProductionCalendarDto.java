package com.nutalig.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseOrderProductionCalendarDto {
    private String purchaseOrderNo;
    private String salesOrderNo;
    private String salesName;
    private String supplierName;
    private CustomerDto customer;
    private LocalDate startDate;
    private LocalDate expectedFinishDate;
    private LocalDate actualFinishDate;
    private String status;
    private Boolean overdue;
}
