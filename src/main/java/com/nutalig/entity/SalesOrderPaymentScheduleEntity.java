package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderPaymentType;
import com.nutalig.constant.SalesOrderPaymentScheduleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "sales_order_payment_schedule")
@ToString(onlyExplicitlyIncluded = true)
public class SalesOrderPaymentScheduleEntity extends AuditDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_no", referencedColumnName = "sales_order_no", nullable = false)
    @ToString.Exclude
    private SalesOrderEntity salesOrder;

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 30, nullable = false)
    private PurchaseOrderPaymentType paymentType;

    @Column(name = "percentage", precision = 8, scale = 4, nullable = false)
    private BigDecimal percentage;

    @Column(name = "expected_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal expectedAmount;

    @Column(name = "paid_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "outstanding_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private SalesOrderPaymentScheduleStatus status = SalesOrderPaymentScheduleStatus.UNINVOICED;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalesOrderPaymentScheduleEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
