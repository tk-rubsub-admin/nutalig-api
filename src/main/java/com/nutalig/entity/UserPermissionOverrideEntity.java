package com.nutalig.entity;

import com.nutalig.constant.Effect;
import com.nutalig.entity.id.UserPermissionOverrideId;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Entity(name = "UserPermissionOverride")
@Table(name = "user_permission_override")
@IdClass(UserPermissionOverrideId.class)
public class UserPermissionOverrideEntity {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Id
    @Column(name = "permission_code", length = 64)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false)
    private Effect effect;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
