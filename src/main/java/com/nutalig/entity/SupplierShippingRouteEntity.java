package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "supplier_shipping_route")
public class SupplierShippingRouteEntity extends AuditDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_shipping_id", nullable = false)
    private SupplierShippingEntity supplierShipping;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_route_id", nullable = false)
    private ShippingRouteEntity shippingRoute;

    @Column(length = 10)
    private String currency;

    @Column(name = "base_cost", precision = 18, scale = 4)
    private BigDecimal baseCost;

    @Column(name = "lead_time_day_min")
    private Integer leadTimeDayMin;

    @Column(name = "lead_time_day_max")
    private Integer leadTimeDayMax;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(length = 1000)
    private String remark;

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierShippingRouteEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
