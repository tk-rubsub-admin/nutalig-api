package com.nutalig.controller.customer.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateCustomerRequest {

    private String customerName;
    private String customerType;
    private String email;
    private String taxId;
    private String companyName;
    private String branchNumber;
    private String branchName;
    private String creditTerm;
    private CreateCustomerAddressRequest address;
    private List<CreateCustomerContactRequest> contacts;
    private String salesAccount;
    private String coSalesAccount;
}
