package com.nutalig.controller.rfq.request;

import lombok.Data;

@Data
public class CloseRfqRequest {

    private String rfqId;
    private String remark;
}
