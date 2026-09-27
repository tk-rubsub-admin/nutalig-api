package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderMilestoneCode;
import com.nutalig.constant.PurchaseOrderMilestoneStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "purchase_order_milestone",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_po_milestone_po_code",
                columnNames = {"purchase_order_no", "milestone_code"}
        )
)
public class PurchaseOrderMilestoneEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_code", nullable = false, length = 50)
    private PurchaseOrderMilestoneCode milestoneCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderMilestoneStatus status = PurchaseOrderMilestoneStatus.PENDING;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "actual_at")
    private ZonedDateTime actualAt;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
