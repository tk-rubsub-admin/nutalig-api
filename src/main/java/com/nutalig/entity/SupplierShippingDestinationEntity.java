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
@Table(name = "supplier_shipping_destination")
@ToString(onlyExplicitlyIncluded = true)
public class SupplierShippingDestinationEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_shipping_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private SupplierShippingEntity supplierShipping;

    @Column(name = "destination_code", length = 100)
    private String destinationCode;

    @Column(name = "destination_name", nullable = false, length = 255)
    private String destinationName;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "province", length = 255)
    private String province;

    @Column(name = "district", length = 255)
    private String district;

    @Column(name = "subdistrict", length = 255)
    private String subdistrict;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "full_address", length = 1000)
    private String fullAddress;

    @Column(name = "additional_cost", precision = 18, scale = 4)
    private BigDecimal additionalCost;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierShippingDestinationEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
