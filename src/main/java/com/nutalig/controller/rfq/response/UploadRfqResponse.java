package com.nutalig.controller.rfq.response;

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
public class UploadRfqResponse {
    private int totalRows;
    private int createdCount;
    private int failedCount;

    @Builder.Default
    private List<UploadRfqErrorResponse> errors = new ArrayList<>();
}
