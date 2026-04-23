package com.nutalig.controller.quotation.request;

import com.nutalig.constant.QuotationStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SearchQuotationRequest {
    private String docNoEqual;
    private LocalDate docDateStart;
    private LocalDate docDateEnd;
    private String customerIdEqual;
    private QuotationStatus statusEqual;
}
