package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@Table(name = "customer_contact")
public class CustomerContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "contact_name")
    @ToString.Include
    private String contactName;

    @Column(name = "contact_number")
    @ToString.Include
    private String contactNumber;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

}
