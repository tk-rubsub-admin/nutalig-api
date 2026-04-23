package com.nutalig.entity;

import com.nutalig.constant.EmployeeStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "Employee")
@Table(name = "employee")
public class EmployeeEntity {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "first_name_th")
    private String firstNameTh;

    @Column(name = "last_name_th")
    private String lastNameTh;

    @Column(name = "nick_name")
    private String nickName;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'POSITION'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "position", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity position;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EmployeeStatus status;

    @Column(name = "additional")
    private String additional;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'TEAM'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "team", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity team;

    @OneToMany(mappedBy = "salesEmployee", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<EmployeeProcurementMappingEntity> procurementMappings = new ArrayList<>();

    @OneToMany(mappedBy = "procurementEmployee", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<EmployeeProcurementMappingEntity> salesMappings = new ArrayList<>();

}
