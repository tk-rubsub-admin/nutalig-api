package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.nutalig.constant.QcTypes.*;

@Getter
@Setter
@Entity
@Table(name = "qc_route")
public class QcRouteEntity extends AuditDateEntity {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type")
    private RouteType routeType;

    @Column(name = "qc_material_group_code")
    private String qcMaterialGroupCode;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
