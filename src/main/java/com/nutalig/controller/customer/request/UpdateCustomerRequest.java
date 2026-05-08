package com.nutalig.controller.customer.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCustomerRequest {

    private String customerType;
    private String customerName;
    private String email;
    private String taxId;
    private String branchNumber;
    private String branchName;
    private String creditTerm;
    private String salesAccount;
    private String coSalesAccount;

}
