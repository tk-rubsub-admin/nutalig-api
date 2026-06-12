package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "search_screen")
@Entity(name = "SearchScreen")
public class SearchScreenEntity extends AuditDateEntity {

    @Id
    @Column(name = "screen_code")
    private String screenCode;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "module_code")
    private String moduleCode;

    @Column(name = "active")
    private Boolean active;
}
