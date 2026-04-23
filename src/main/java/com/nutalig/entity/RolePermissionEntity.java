package com.nutalig.entity;

import com.nutalig.entity.id.RolePermissionId;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "role_permission")
@Entity(name = "RolePermission")
@IdClass(RolePermissionId.class)
public class RolePermissionEntity {

    @Id
    @Column(name = "role_code")
    private String roleCode;

    @Id
    @Column(name = "permission_code")
    private String permissionCode;
}
