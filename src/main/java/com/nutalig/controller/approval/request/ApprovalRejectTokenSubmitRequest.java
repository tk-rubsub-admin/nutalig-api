package com.nutalig.controller.approval.request;

import lombok.Data;

@Data
public class ApprovalRejectTokenSubmitRequest {
    private String token;
    private String reason;
}
