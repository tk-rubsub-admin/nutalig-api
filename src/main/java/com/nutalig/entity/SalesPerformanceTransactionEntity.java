package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.SalesPerformanceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "SalesPerformanceTransaction")
@Table(name = "sales_performance_transaction")
public class SalesPerformanceTransactionEntity {

    @Id
    @Column(name = "invoice_payment_id", nullable = false)
    private Long invoicePaymentId;

    @Column(name = "sales_order_no", nullable = false, length = 50)
    private String salesOrderNo;

    @Column(name = "invoice_no", nullable = false, length = 50)
    private String invoiceNo;

    @Column(name = "sales_id", nullable = false, length = 255)
    private String salesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_type", nullable = false, length = 30)
    private SalesPerformanceType performanceType;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal amount;
}
