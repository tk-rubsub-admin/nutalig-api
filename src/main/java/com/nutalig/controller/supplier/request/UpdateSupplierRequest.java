package com.nutalig.controller.supplier.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateSupplierRequest {
    private String fullAddress;
    private List<CreateSupplierContactRequest> contacts;
}
