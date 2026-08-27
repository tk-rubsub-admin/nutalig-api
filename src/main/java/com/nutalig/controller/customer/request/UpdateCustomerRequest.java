package com.nutalig.controller.customer.request;

import lombok.Data;

import java.util.List;

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
    private List<CustomerBranchRequest> branches;
    private String creditTerm;
    private String paymentTerm;
    private String billingCondition;
    private String paymentCycle;
    private String salesAccount;
    private List<String> salesAccounts;
    private String coSalesAccount;

}
