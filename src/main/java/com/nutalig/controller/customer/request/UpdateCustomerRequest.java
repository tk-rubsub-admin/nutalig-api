package com.nutalig.controller.customer.request;

import lombok.Data;

@Data
public class UpdateCustomerRequest {

    private String customerType;
    private String customerTier;
    private String customerSegment;
    private String customerName;
    private String email;
    private String taxId;
    private String branchNumber;
    private String branchName;
    private String creditTerm;
    private String salesAccount;
    private String coSalesAccount;

}
