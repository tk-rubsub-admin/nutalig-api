package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Table(name = "subdistrict")
@Entity(name = "SubDistrict")
public class SubDistrictEntity extends AuditDateEntity {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "zip_code")
    private String zipCode;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", referencedColumnName = "id", insertable = false, updatable = false)
    private DistrictEntity district;

}
