package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.ZonedDateTime;

@Data
@Entity
@Table(
        name = "employee_procurement_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_emp_proc_map",
                        columnNames = {"sales_employee_id", "procurement_employee_id"}
                )
        }
)
public class EmployeeProcurementMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_employee_id", referencedColumnName = "employee_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private EmployeeEntity salesEmployee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procurement_employee_id", referencedColumnName = "employee_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private EmployeeEntity procurementEmployee;

    @Column(name = "updated_date", nullable = false)
    private ZonedDateTime updatedDate;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "is_default")
    private Boolean isDefault;
}