package com.nutalig.entity.id;

import lombok.Data;

import java.io.Serializable;

@Data
public class RolePermissionId implements Serializable {
    private String roleCode;
    private String permissionCode;
}
