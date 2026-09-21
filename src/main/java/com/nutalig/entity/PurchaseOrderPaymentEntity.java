package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PaymentMethod;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.constant.PurchaseOrderPaymentType;
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
@Table(name = "purchase_order_payment")
@ToString(onlyExplicitlyIncluded = true)
public class PurchaseOrderPaymentEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    @ToString.Exclude
    private PurchaseOrderEntity purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", referencedColumnName = "id")
    @ToString.Exclude
    private PurchaseOrderPaymentScheduleEntity schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 30, nullable = false)
    private PurchaseOrderPaymentType paymentType;

    @Column(name = "installment_no")
    private Integer installmentNo;

    @Column(name = "payment_date", nullable = false)
    private ZonedDateTime paymentDate;

    @Column(name = "amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10, nullable = false)
    private Currency currency;

    @Column(name = "exchange_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "amount_thb", precision = 18, scale = 5, nullable = false)
    private BigDecimal amountThb;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30, nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "transfer_reference", length = 255)
    private String transferReference;

    @Column(name = "cheque_bank", length = 255)
    private String chequeBank;

    @Column(name = "cheque_no", length = 100)
    private String chequeNo;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "cheque_branch", length = 255)
    private String chequeBranch;

    @Column(name = "remark", length = 2000)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PurchaseOrderPaymentStatus status;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity approvedBy;

    @Column(name = "approved_date")
    private ZonedDateTime approvedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity rejectedBy;

    @Column(name = "rejected_date")
    private ZonedDateTime rejectedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity voidedBy;

    @Column(name = "voided_date")
    private ZonedDateTime voidedDate;

    @Column(name = "void_reason", length = 2000)
    private String voidReason;

    @Column(name = "request_key", length = 100, unique = true)
    private String requestKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

    @OneToMany(mappedBy = "purchaseOrderPayment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @ToString.Exclude
    private Set<PurchaseOrderPaymentAttachmentEntity> attachments = new LinkedHashSet<>();

    public void addAttachment(PurchaseOrderPaymentAttachmentEntity attachment) {
        if (attachment == null) return;
        attachments.add(attachment);
        attachment.setPurchaseOrderPayment(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderPaymentEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
