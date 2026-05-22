package com.nutalig.dto;

import lombok.Data;

@Data
public class SupplierContactDto {
    private String id;
    private String contactName;
    private String contactNumber;
    private String wechat;
    private Boolean isDefault;
}
