package com.nutalig.controller.customer.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadCustomerErrorResponse {
    private int rowNumber;
    private String customerName;
    private String message;
}
