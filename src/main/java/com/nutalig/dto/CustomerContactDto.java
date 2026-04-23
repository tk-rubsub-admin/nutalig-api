package com.nutalig.dto;

import lombok.Data;

@Data
public class CustomerContactDto {
    private String id;
    private String contactName;
    private String contactNumber;
    private String remark;
    private Boolean isDefault;
}
