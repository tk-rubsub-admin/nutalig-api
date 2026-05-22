package com.nutalig.controller.rfq.request;

import com.nutalig.constant.RFQStatus;
import lombok.Data;

import java.util.List;

@Data
public class SearchRFQRequest {

    private String id;
    private RFQStatus status;
    private List<RFQStatus> statuses;
    private String customerId;
    private String salesId;
    private String orderTypeCode;
    private String keyword;
}
