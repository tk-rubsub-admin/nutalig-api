package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderProofStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "purchase_order_proof_revision",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_po_proof_revision",
                columnNames = {"proof_id", "revision_no"}
        )
)
public class PurchaseOrderProofRevisionEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proof_id", nullable = false)
    private PurchaseOrderProofEntity proof;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "procurement_note", columnDefinition = "TEXT")
    private String procurementNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderProofStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", referencedColumnName = "id", nullable = false)
    private UserEntity requestedByUser;

    @Column(name = "requested_at", nullable = false)
    private ZonedDateTime requestedAt;

    @Column(name = "due_date")
    private ZonedDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acted_by_user_id", referencedColumnName = "id")
    private UserEntity actedByUser;

    @Column(name = "acted_at")
    private ZonedDateTime actedAt;

    @Column(name = "sales_comment", columnDefinition = "TEXT")
    private String salesComment;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequestEntity approvalRequest;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    private List<PurchaseOrderProofAttachmentEntity> attachments = new ArrayList<>();

    public void addAttachment(PurchaseOrderProofAttachmentEntity attachment) {
        attachments.add(attachment);
        attachment.setRevision(this);
    }
}
