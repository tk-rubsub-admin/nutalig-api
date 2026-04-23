package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "permission")
public class PermissionEntity {

    @Id
    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "group")
    private String group;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;
}