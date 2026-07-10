package com.nutalig.dto;

import com.nutalig.constant.ApprovalAuditEventType;
import com.nutalig.constant.ApprovalSource;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ApprovalRequestAuditLogDto {
    private Long id;
    private ApprovalAuditEventType eventType;
    private String actorUserId;
    private String actorDisplayName;
    private String actorLineUserId;
    private ApprovalSource source;
    private String summary;
    private String detailJson;
    private ZonedDateTime createdDate;
}
