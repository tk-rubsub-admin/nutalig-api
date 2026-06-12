package com.nutalig.entity.id;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoleSearchFieldVisibilityId implements Serializable {
    private String roleCode;
    private String screenCode;
    private String fieldCode;
}
