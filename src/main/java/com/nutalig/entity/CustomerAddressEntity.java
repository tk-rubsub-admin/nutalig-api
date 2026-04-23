package com.nutalig.entity;

import com.nutalig.constant.AddressType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@Table(name = "customer_address")
public class CustomerAddressEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", length = 20, nullable = false)
    private AddressType addressType;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "subdistrict", length = 120)
    private String subdistrict;

    @Column(name = "district", length = 120)
    private String district;

    @Column(name = "province", length = 120)
    private String province;

    @Column(name = "postcode", length = 10)
    private String postcode;

    @Column(name = "country", length = 80)
    private String country;
}