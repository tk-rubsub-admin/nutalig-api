package com.nutalig.controller.rfq.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadRfqErrorResponse {
    private int rowNumber;
    private String salesId;
    private String description;
    private String message;
}
