package com.nutalig.entity;

import com.nutalig.constant.InvoicePaymentStatus;
import com.nutalig.constant.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "invoice_payment")
@ToString(onlyExplicitlyIncluded = true)
public class InvoicePaymentEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_no", referencedColumnName = "invoice_no", nullable = false)
    @ToString.Exclude
    private InvoiceEntity invoice;

    @Column(name = "payment_date", nullable = false)
    private ZonedDateTime paymentDate;

    @Column(name = "amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30, nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "cheque_bank", length = 255)
    private String chequeBank;

    @Column(name = "cheque_no", length = 100)
    private String chequeNo;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "cheque_branch", length = 255)
    private String chequeBranch;

    @Column(name = "slip_file_name", length = 255)
    private String slipFileName;

    @Column(name = "slip_file_url", length = 500)
    private String slipFileUrl;

    @Column(name = "receipt_no", length = 50)
    private String receiptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private InvoicePaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoicePaymentEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
