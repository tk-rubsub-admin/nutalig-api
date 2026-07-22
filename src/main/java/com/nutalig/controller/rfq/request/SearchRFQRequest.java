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
    private String rfqTypeCode;
    private String productFamily;
    private String productSubtype1;
    private String productMaterial;
    private String orderType;
    private String orderTypeCode;
    private String keyword;
    private LocalDate requestedDateStart;
    private LocalDate requestedDateEnd;
    private Boolean isAccept;
    private Boolean prioritizeApprovedUrgent;
    private Boolean isCreatedPurchaseOrder;

}
