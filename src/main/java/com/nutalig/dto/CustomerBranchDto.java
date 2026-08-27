package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CustomerBranchDto {
    private String branchCode;
    private String branchName;
    private Boolean isDefault;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
