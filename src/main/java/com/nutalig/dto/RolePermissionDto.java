package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionDto {

    private String roleCode;
    private String roleNameTh;
    private String roleNameEn;
    private List<PermissionDto> permissions;
}
