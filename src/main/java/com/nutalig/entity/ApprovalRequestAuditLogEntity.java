package com.nutalig.entity;

import com.nutalig.constant.ApprovalAuditEventType;
import com.nutalig.constant.ApprovalSource;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_request_audit_log")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApprovalRequestAuditLogEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequestEntity approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_step_id")
    private ApprovalRequestStepEntity approvalRequestStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ApprovalAuditEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", referencedColumnName = "id")
    private UserEntity actorUser;

    @Column(name = "actor_line_user_id", length = 255)
    private String actorLineUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private ApprovalSource source;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "detail_json", columnDefinition = "LONGTEXT")
    private String detailJson;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
