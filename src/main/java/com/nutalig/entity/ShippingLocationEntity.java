package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "shipping_location")
public class ShippingLocationEntity extends AuditDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "location_type_group_code", referencedColumnName = "group_code", insertable = false, updatable = false),
            @JoinColumn(name = "location_type", referencedColumnName = "code", insertable = false, updatable = false)
    })
    private SystemConfigEntity locationType;

    @Column(name = "location_type", length = 30)
    private String locationTypeCode;

    @Column(name = "location_type_group_code", length = 100)
    private String locationTypeGroupCode;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(length = 255)
    private String province;

    @Column(length = 255)
    private String district;

    @Column(length = 1000)
    private String address;

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShippingLocationEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
