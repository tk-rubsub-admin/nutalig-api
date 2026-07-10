package com.nutalig.entity;

import com.nutalig.constant.ApprovalSource;
import com.nutalig.constant.ApprovalStepStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "approval_request_step")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApprovalRequestStepEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequestEntity approvalRequest;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id", referencedColumnName = "id")
    private UserEntity approverUser;

    @Column(name = "approver_role_code", length = 50)
    private String approverRoleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    @Column(name = "acted_at")
    private ZonedDateTime actedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acted_by_user_id", referencedColumnName = "id")
    private UserEntity actedByUser;

    @Column(name = "acted_by_line_user_id", length = 255)
    private String actedByLineUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_channel", length = 30)
    private ApprovalSource actionChannel;

    @Column(name = "approve_action_key", length = 64)
    private String approveActionKey;

    @Column(name = "reject_action_key", length = 64)
    private String rejectActionKey;

    @Column(name = "line_message_id", length = 255)
    private String lineMessageId;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
