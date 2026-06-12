package com.nutalig.entity;

import com.nutalig.entity.id.RoleSearchFieldVisibilityId;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "role_search_field_visibility")
@Entity(name = "RoleSearchFieldVisibility")
@IdClass(RoleSearchFieldVisibilityId.class)
public class RoleSearchFieldVisibilityEntity extends AuditDateEntity {

    @Id
    @Column(name = "role_code")
    private String roleCode;

    @Id
    @Column(name = "screen_code")
    private String screenCode;

    @Id
    @Column(name = "field_code")
    private String fieldCode;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "updated_by")
    private String updatedBy;
}
