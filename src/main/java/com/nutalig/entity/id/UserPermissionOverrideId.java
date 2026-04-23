package com.nutalig.entity.id;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPermissionOverrideId implements Serializable {
    private String userId;
    private String permissionCode;
}
