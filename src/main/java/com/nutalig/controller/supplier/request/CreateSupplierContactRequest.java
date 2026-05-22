package com.nutalig.controller.supplier.request;

import lombok.Data;

@Data
public class CreateSupplierContactRequest {

    private String contactName;
    private String contactNumber;
    private String wechat;
    private Boolean isDefault;
}
