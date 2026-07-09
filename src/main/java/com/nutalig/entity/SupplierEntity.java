package com.nutalig.entity;

import com.nutalig.constant.Status;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "supplier")
@Entity(name = "Supplier")
public class SupplierEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(generator = "supplierIdGenerator")
    @GenericGenerator(name = "supplierIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-SUP"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ToString.Include
    @Column(name = "supplier_name")
    private String supplierName;

    @ToString.Include
    @Column(name = "supplier_code")
    private String supplierCode;

    @Column(name = "supplier_email")
    private String supplierEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @ToString.Include
    private Status status;

    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "full_address_en")
    private String fullAddressEn;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "province")
    private String province;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "town")
    private String town;

    @Column(name = "street")
    private String street;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "additional")
    private String additional;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierContactEntity> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("productFamilyCode ASC, productMaterialCode ASC")
    private List<SupplierCapabilityEntity> capabilities = new ArrayList<>();


    public void addContact(SupplierContactEntity contact) {
        contacts.add(contact);
        contact.setSupplier(this);
    }

    public void addCapability(SupplierCapabilityEntity capability) {
        capabilities.add(capability);
        capability.setSupplier(this);
    }

}
