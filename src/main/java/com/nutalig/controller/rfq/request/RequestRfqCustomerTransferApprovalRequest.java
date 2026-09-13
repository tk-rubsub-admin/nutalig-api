package com.nutalig.controller.rfq.request;

import lombok.Data;

@Data
public class RequestRfqCustomerTransferApprovalRequest {
    private String targetCustomerId;
    private String reason;
}
