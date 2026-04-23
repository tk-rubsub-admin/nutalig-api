package com.nutalig.controller.quotation.response;

import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.QuotationDto;
import lombok.Data;

import java.util.List;

@Data
public class SearchQuotationResponse {
    private List<QuotationDto> quotationList;
    private Pagination pagination;
}
