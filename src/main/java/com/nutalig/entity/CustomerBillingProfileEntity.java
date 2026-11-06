package com.nutalig.entity;

import com.nutalig.constant.BillingProfileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "customer_billing_profile",
        indexes = {
                @Index(name = "idx_customer_billing_profile_customer", columnList = "customer_id"),
                @Index(name = "idx_customer_billing_profile_default", columnList = "customer_id, is_default, active")
        }
)
public class CustomerBillingProfileEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    @Column(name = "profile_name", length = 100)
    private String profileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_profile_type", nullable = false, length = 20)
    private BillingProfileType billingProfileType = BillingProfileType.VAT;

    @Column(name = "billing_name", nullable = false, length = 255)
    private String billingName;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "branch_name", length = 255)
    private String branchName;

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
    private String country = "Thailand";

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 100)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
