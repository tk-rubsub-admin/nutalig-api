package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@Table(name = "supplier_contact")
public class SupplierContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id", nullable = false)
    private SupplierEntity supplier;

    @Column(name = "contact_name")
    @ToString.Include
    private String contactName;

    @Column(name = "contact_number")
    @ToString.Include
    private String contactNumber;

    @Column(name = "wechat")
    @ToString.Include
    private String wechat;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

}
