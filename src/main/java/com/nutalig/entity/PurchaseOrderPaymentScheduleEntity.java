package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderPaymentScheduleStatus;
import com.nutalig.constant.PurchaseOrderPaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_payment_schedule")
@ToString(onlyExplicitlyIncluded = true)
public class PurchaseOrderPaymentScheduleEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    @ToString.Exclude
    private PurchaseOrderEntity purchaseOrder;

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 30, nullable = false)
    private PurchaseOrderPaymentType paymentType;

    @Column(name = "percentage", precision = 8, scale = 4, nullable = false)
    private BigDecimal percentage;

    @Column(name = "expected_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal expectedAmount;

    @Column(name = "expected_amount_thb", precision = 18, scale = 5, nullable = false)
    private BigDecimal expectedAmountThb;

    @Column(name = "paid_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount_thb", precision = 18, scale = 5, nullable = false)
    private BigDecimal paidAmountThb = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PurchaseOrderPaymentScheduleStatus status = PurchaseOrderPaymentScheduleStatus.UNPAID;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @OneToMany(mappedBy = "schedule")
    @OrderBy("createdDate desc, id desc")
    @ToString.Exclude
    private Set<PurchaseOrderPaymentEntity> payments = new LinkedHashSet<>();

    public void addPayment(PurchaseOrderPaymentEntity payment) {
        if (payment == null) return;
        payments.add(payment);
        payment.setSchedule(this);
    }

    public void removePayment(PurchaseOrderPaymentEntity payment) {
        if (payment == null) return;
        payments.remove(payment);
        if (payment.getSchedule() == this) payment.setSchedule(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderPaymentScheduleEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
