package com.nutalig.dto;

import com.nutalig.constant.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerDto {

    private String id;
    private String customerName;
    private Status status;
    private SystemConfigDto customerType;
    private SystemConfigDto customerCreditTerm;
    private SystemConfigDto customerPaymentTerm;
    private SystemConfigDto customerTier;
    private SystemConfigDto customerSegment;
    private String taxId;
    private String companyName;
    private String branchNumber;
    private String branchName;
    private String email;
    private String salesAccount;
    private List<String> salesAccounts;
    private String coSalesAccount;
    private BigDecimal totalSalesOrderAmount;
    private String createdBy;
    private String updatedBy;
    private List<CustomerAddressDto> addresses;
    private List<CustomerContactDto> contacts;

    public CustomerDto(String id, String customerName) {
        this.id = id;
        this.customerName = customerName;
    }
}
