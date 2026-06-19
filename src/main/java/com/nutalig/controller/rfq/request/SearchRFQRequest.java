package com.nutalig.controller.rfq.request;

import com.nutalig.constant.RfqStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SearchRFQRequest {

    private String id;
    private RfqStatus status;
    private List<RfqStatus> statuses;
    private String customerId;
    private String salesId;
    private String procurementId;
    private String productFamily;
    private String orderType;
    private String orderTypeCode;
    private String keyword;
    private LocalDate requestedDateStart;
    private LocalDate requestedDateEnd;

}
