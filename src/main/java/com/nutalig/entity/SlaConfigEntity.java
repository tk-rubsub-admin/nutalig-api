package com.nutalig.entity;

import com.nutalig.constant.SlaDayType;
import com.nutalig.constant.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "sla_config")
public class SlaConfigEntity {

    @Id
    @Column(name = "sla_code", nullable = false, unique = true, length = 100)
    private String slaCode;

    @Column(name = "sla_name", nullable = false, length = 255)
    private String slaName;

    @Column(name = "target_days", nullable = false)
    private Integer targetDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 30)
    private SlaDayType dayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "updated_date", nullable = false)
    private ZonedDateTime updatedDate;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updatedBy;
}
