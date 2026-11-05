package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "invoice_payment_allocation")
@ToString(onlyExplicitlyIncluded = true)
public class InvoicePaymentAllocationEntity extends AuditDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_payment_id", nullable = false)
    @ToString.Exclude
    private InvoicePaymentEntity invoicePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_no", referencedColumnName = "invoice_no")
    @ToString.Exclude
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_no", referencedColumnName = "sales_order_no")
    @ToString.Exclude
    private SalesOrderEntity salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_schedule_id", referencedColumnName = "id")
    @ToString.Exclude
    private SalesOrderPaymentScheduleEntity paymentSchedule;

    @Column(name = "allocation_type", nullable = false, length = 30)
    private String allocationType;

    @Column(name = "allocated_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
