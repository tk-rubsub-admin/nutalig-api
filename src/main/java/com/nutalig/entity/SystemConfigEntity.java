package com.nutalig.entity;

import com.nutalig.entity.id.SystemConfigId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "system_config")
@Entity(name = "SystemConfig")
public class SystemConfigEntity {

    @EmbeddedId
    private SystemConfigId id;
    @Column(name = "name_th")
    private String nameTh;
    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "sort")
    private Integer sort;
}
