package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.nutalig.constant.QcTypes.*;

@Getter
@Setter
@Entity
@Table(name = "product_material_qc_rule")
public class ProductMaterialQcRuleEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "taxonomy_revision")
    private String taxonomyRevision;

    @Column(name = "product_family_code")
    private String productFamilyCode;

    @Column(name = "product_material_code")
    private String productMaterialCode;

    @Column(name = "product_subtype1_code")
    private String productSubtype1Code;

    @Column(name = "qc_route_code")
    private String qcRouteCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "purchasing_confirmation_policy")
    private PurchasingConfirmationPolicy purchasingConfirmationPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RuleStatus status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
