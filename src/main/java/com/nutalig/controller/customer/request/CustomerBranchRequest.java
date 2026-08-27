package com.nutalig.controller.customer.request;

import lombok.Data;

@Data
public class CustomerBranchRequest {
    private String branchCode;
    private String branchName;
    private Boolean isDefault;
}
