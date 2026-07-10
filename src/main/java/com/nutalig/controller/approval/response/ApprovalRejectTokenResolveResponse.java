package com.nutalig.controller.approval.response;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ApprovalRequestStatus;
import com.nutalig.constant.ApprovalRequestType;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Map;

@Value
@Builder
public class ApprovalRejectTokenResolveResponse {
    Long requestId;
    Long stepId;
    String requestNo;
    String title;
    ActivityEntityType entityType;
    String referenceId;
    ApprovalRequestType requestType;
    ApprovalRequestStatus status;
    Integer currentStepNo;
    String approverRoleCode;
    String approverDisplayName;
    String rejectReason;
    Map<String, Object> payload;
    ZonedDateTime requestedDate;
    ZonedDateTime actedAt;
}
