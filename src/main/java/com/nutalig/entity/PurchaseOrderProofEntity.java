package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderProofStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "purchase_order_proof",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_po_proof_po_type",
                columnNames = {"purchase_order_no", "proof_type"}
        )
)
public class PurchaseOrderProofEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula = @JoinFormula(
                    value = "'PURCHASE_ORDER_PROOF_TYPE'",
                    referencedColumnName = "group_code"
            )),
            @JoinColumnOrFormula(column = @JoinColumn(
                    name = "proof_type",
                    referencedColumnName = "code",
                    nullable = false
            ))
    })
    private SystemConfigEntity proofType;

    @Column(name = "is_required", nullable = false)
    private Boolean required = Boolean.TRUE;

    @Column(name = "current_revision", nullable = false)
    private Integer currentRevision = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderProofStatus status = PurchaseOrderProofStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_sales_user_id", referencedColumnName = "id", nullable = false)
    private UserEntity assignedSalesUser;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "proof", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("revisionNo desc")
    private List<PurchaseOrderProofRevisionEntity> revisions = new ArrayList<>();

    public void addRevision(PurchaseOrderProofRevisionEntity revision) {
        revisions.add(revision);
        revision.setProof(this);
    }
}
