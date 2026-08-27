package com.nutalig.dto;

import lombok.Data;

@Data
public class QuotationCustomerSnapshotDto {
    private String customerName;
    private String taxId;
    private String branchCode;
    private String branchName;
    private String address;
    private String contactName;
    private String contactNumber;
}
