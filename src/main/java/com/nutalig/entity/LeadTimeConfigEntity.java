package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lead_time_config")
public class LeadTimeConfigEntity {

    @Id
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "name_th", nullable = false, length = 255)
    private String nameTh;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
