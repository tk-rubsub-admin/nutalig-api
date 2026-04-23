package com.nutalig.controller.customer.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCustomerRequest {
    private String customerName;
    private String contactNumber1;
    private String contactNumber2;
    private String contactName;
    private String lineId;
    private String gender;
    private LocalDate dateOfBirth;
    private String type;
    private String taxId;
    private String companyName;
    private String companyBranchCode;
    private String companyBranchName;
    private String orderChannel;
    private String purchaseChannel;
    private String behavior;
    private String creditTerm;
    private String sendingBillMethod;
    private String customerRank;
    private String billingHeader;
    private String address;
    private String addressAmphure;
    private String addressProvince;
    private String addressTumbon;
}
