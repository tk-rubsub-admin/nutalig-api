package com.nutalig.controller.rfq.request;

import com.nutalig.constant.RFQStatus;
import lombok.Data;

@Data
public class SearchRFQRequest {

    private String id;
    private RFQStatus status;
    private String customerId;
    private String salesId;
    private String orderTypeCode;
    private String keyword;
}
