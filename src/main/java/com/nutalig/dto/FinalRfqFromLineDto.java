package com.nutalig.dto;

import com.nutalig.controller.rfq.request.CreateRequestPriceAdditionalCostRequest;
import com.nutalig.controller.rfq.request.CreateRequestPriceDetailRequest;
import lombok.Data;

import java.util.List;

@Data
public class FinalRfqFromLineDto {

    private String rfqId;
    private List<CreateRequestPriceDetailRequest> details;
    private List<CreateRequestPriceAdditionalCostRequest> additionalCosts;
}
