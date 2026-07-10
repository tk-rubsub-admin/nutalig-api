package com.nutalig.entity;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ApprovalRequestStatus;
import com.nutalig.constant.ApprovalRequestType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "approval_request")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApprovalRequestEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "request_no", nullable = false, unique = true, length = 50)
    @ToString.Include
    private String requestNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private ActivityEntityType entityType;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private ApprovalRequestType requestType;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApprovalRequestStatus status = ApprovalRequestStatus.PENDING;

    @Column(name = "current_step_no")
    private Integer currentStepNo;

    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @Column(name = "requested_date", nullable = false)
    private ZonedDateTime requestedDate;

    @Column(name = "approved_date")
    private ZonedDateTime approvedDate;

    @Column(name = "rejected_date")
    private ZonedDateTime rejectedDate;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo ASC, id ASC")
    private List<ApprovalRequestStepEntity> steps = new ArrayList<>();

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC, id ASC")
    private List<ApprovalRequestAuditLogEntity> auditLogs = new ArrayList<>();

    public void addStep(ApprovalRequestStepEntity step) {
        steps.add(step);
        step.setApprovalRequest(this);
    }

    public void addAuditLog(ApprovalRequestAuditLogEntity auditLog) {
        auditLogs.add(auditLog);
        auditLog.setApprovalRequest(this);
    }
}
