package com.nutalig.controller.customer.request;

import lombok.Data;

@Data
public class CreateCustomerContactRequest {
    private String contactName;
    private String contactNumber;
}
