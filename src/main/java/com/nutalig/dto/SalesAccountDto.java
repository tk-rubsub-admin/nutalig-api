package com.nutalig.dto;

import lombok.Data;

@Data
public class SalesAccountDto {
    private String salesId;
    private SystemConfigDto type;
    private String name;
    private String nickname;
    private String mobileNo;
    private String bankAccountNo;
    private String bankName;
    private String bankAccountName;
    private SystemConfigDto team;
}
