package com.nutalig.controller.sales.request;

import lombok.Data;

@Data
public class UpdateSalesRequest {

    private String type;
    private String name;
    private String nickname;
    private String mobileNo;
    private String bankAccountNo;
    private String bankName;
    private String bankAccountName;
    private String team;
}
