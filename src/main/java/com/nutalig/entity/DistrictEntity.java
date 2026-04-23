package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Data
@Table(name = "district")
@Entity(name = "District")
public class DistrictEntity extends AuditDateEntity {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ProvinceEntity province;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "district")
    Set<SubDistrictEntity> subDistrictEntities;

}
