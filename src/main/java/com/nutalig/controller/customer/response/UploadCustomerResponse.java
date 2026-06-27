package com.nutalig.controller.customer.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadCustomerResponse {
    private int totalRows;
    private int createdCount;
    private int skippedCount;
    private int failedCount;

    @Builder.Default
    private List<UploadCustomerErrorResponse> errors = new ArrayList<>();
}
