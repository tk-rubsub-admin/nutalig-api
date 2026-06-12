package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleSearchFieldVisibilityDto {
    private String roleCode;
    private String roleNameTh;
    private String roleNameEn;
    private String screenCode;
    private List<SearchFieldDto> fields;
}
