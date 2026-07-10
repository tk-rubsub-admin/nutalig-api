package com.nutalig.dto;

import com.nutalig.constant.ApprovalSource;
import com.nutalig.constant.ApprovalStepStatus;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ApprovalRequestStepDto {
    private Long id;
    private Integer stepNo;
    private String approverUserId;
    private String approverDisplayName;
    private String approverRoleCode;
    private ApprovalStepStatus status;
    private ZonedDateTime sentAt;
    private ZonedDateTime actedAt;
    private String actedByUserId;
    private String actedByDisplayName;
    private String actedByLineUserId;
    private ApprovalSource actionChannel;
    private String lineMessageId;
    private String rejectReason;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
