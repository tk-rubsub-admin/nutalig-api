package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.ShippingMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "supplier_shipping")
@ToString(onlyExplicitlyIncluded = true)
public class SupplierShippingEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false, length = 20)
    private ShippingMethod shippingMethod;

    @Column(name = "shipping_name", length = 255)
    private String shippingName;

    @Column(name = "origin_country_code", length = 10)
    private String originCountryCode;

    @Column(name = "origin_province", length = 255)
    private String originProvince;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @Column(name = "base_cost", precision = 18, scale = 4)
    private BigDecimal baseCost;

    @Column(name = "lead_time_day_min")
    private Integer leadTimeDayMin;

    @Column(name = "lead_time_day_max")
    private Integer leadTimeDayMax;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "car_code")
    private String carCode;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "supplierShipping", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @ToString.Exclude
    private Set<SupplierShippingDestinationEntity> destinations = new LinkedHashSet<>();

    public void addDestination(SupplierShippingDestinationEntity destination) {
        if (destination == null) return;
        destinations.add(destination);
        destination.setSupplierShipping(this);
    }

    public void removeDestination(SupplierShippingDestinationEntity destination) {
        if (destination == null) return;
        destinations.remove(destination);
        destination.setSupplierShipping(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierShippingEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
