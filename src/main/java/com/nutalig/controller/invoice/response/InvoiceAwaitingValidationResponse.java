package com.nutalig.controller.invoice.response;

import com.nutalig.dto.InvoiceDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAwaitingValidationResponse {
    private InvoiceDto invoice;
    private Long paymentId;
}
