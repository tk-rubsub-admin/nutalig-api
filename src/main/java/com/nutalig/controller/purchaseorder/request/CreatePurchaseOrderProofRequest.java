package com.nutalig.controller.purchaseorder.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;

@Data
public class CreatePurchaseOrderProofRequest {
    private String proofType;
    private String title;
    private String procurementNote;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dueDate;
}
