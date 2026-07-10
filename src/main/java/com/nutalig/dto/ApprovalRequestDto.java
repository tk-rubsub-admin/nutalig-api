package com.nutalig.dto;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ApprovalRequestStatus;
import com.nutalig.constant.ApprovalRequestType;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ApprovalRequestDto {
    private Long id;
    private String requestNo;
    private ActivityEntityType entityType;
    private String referenceId;
    private ApprovalRequestType requestType;
    private String templateCode;
    private String title;
    private ApprovalRequestStatus status;
    private Integer currentStepNo;
    private String requestedBy;
    private ZonedDateTime requestedDate;
    private ZonedDateTime approvedDate;
    private ZonedDateTime rejectedDate;
    private String rejectReason;
    private Map<String, Object> payload;
    private List<ApprovalRequestStepDto> steps;
    private List<ApprovalRequestAuditLogDto> auditLogs;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
