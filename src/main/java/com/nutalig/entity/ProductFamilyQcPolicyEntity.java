package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.nutalig.entity.id.ProductFamilyQcPolicyId;

@Getter
@Setter
@Entity
@Table(name = "product_family_qc_policy")
@IdClass(ProductFamilyQcPolicyId.class)
public class ProductFamilyQcPolicyEntity extends AuditDateEntity {

    @Id
    @Column(name = "taxonomy_revision")
    private String taxonomyRevision;

    @Id
    @Column(name = "product_family_code")
    private String productFamilyCode;

    @Column(name = "special_inspection_plan_required")
    private Boolean specialInspectionPlanRequired;

    @Column(name = "special_qc_route_code")
    private String specialQcRouteCode;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
