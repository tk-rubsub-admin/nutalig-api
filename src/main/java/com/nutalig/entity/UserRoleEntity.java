package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "user_role")
@Entity(name = "UserRole")
public class UserRoleEntity {

    @Id
    @Column(name = "role_code")
    private String roleCode;

    @Column(name = "role_name_th")
    private String roleNameTh;

    @Column(name = "role_name_en")
    private String roleNameEn;

}
