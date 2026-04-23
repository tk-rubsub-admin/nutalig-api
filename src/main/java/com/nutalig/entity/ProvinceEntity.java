package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Data
@Table(name = "province")
@Entity(name = "Province")
public class ProvinceEntity extends AuditDateEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "province")
    Set<DistrictEntity> districtEntities;

}
