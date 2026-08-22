package com.nutalig.controller.rfq.request;

import com.nutalig.constant.RequestInfoTo;
import lombok.Data;

@Data
public class RequestRfqInformationRequest {

    private String rfqId;
    private String requestInformation;
    private RequestInfoTo requestTo;
}
