package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderPaymentInstructionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_payment_instruction")
@ToString(onlyExplicitlyIncluded = true)
public class PurchaseOrderPaymentInstructionEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    @ToString.Exclude
    private PurchaseOrderPaymentScheduleEntity schedule;

    @Column(name = "amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_thb", precision = 18, scale = 5)
    private BigDecimal amountThb;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10, nullable = false)
    private Currency currency;

    @Column(name = "exchange_rate", precision = 18, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "manually_confirmed_paid", nullable = false)
    private boolean manuallyConfirmedPaid;

    @Column(name = "confirmed_paid_date")
    private ZonedDateTime confirmedPaidDate;

    @Column(name = "planned_payment_date", nullable = false)
    private LocalDate plannedPaymentDate;

    @Column(name = "paid_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount_thb", precision = 18, scale = 5, nullable = false)
    private BigDecimal paidAmountThb = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PurchaseOrderPaymentInstructionStatus status =
            PurchaseOrderPaymentInstructionStatus.AWAITING_PAYMENT;

    @Column(name = "remark", length = 2000)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

    @OneToMany(mappedBy = "instruction")
    @OrderBy("createdDate desc, id desc")
    @ToString.Exclude
    private Set<PurchaseOrderPaymentEntity> payments = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderPaymentInstructionEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
