package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "receipt_detail")
@ToString(onlyExplicitlyIncluded = true)
public class ReceiptDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_no", referencedColumnName = "receipt_no", nullable = false)
    @ToString.Exclude
    private ReceiptEntity receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_detail_id", referencedColumnName = "id")
    @ToString.Exclude
    private InvoiceDetailEntity invoiceDetail;

    @Column(name = "line_no")
    private Integer lineNo;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "capacity", length = 255)
    private String capacity;

    @Column(name = "size", length = 255)
    private String size;

    @Column(name = "spec", length = 2000)
    private String spec;

    @Column(name = "unit_price", precision = 18, scale = 5)
    private BigDecimal unitPrice;

    @Column(name = "quantity", precision = 18, scale = 5)
    private BigDecimal quantity;

    @Column(name = "amount", precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReceiptDetailEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? Objects.hash(id) : System.identityHashCode(this);
    }
}
