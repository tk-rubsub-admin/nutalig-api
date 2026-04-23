package com.nutalig.entity;

import com.nutalig.constant.SalesType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

@Data
@Entity
@Table(name = "sales")
public class SalesEntity {

    @Id
    @Column(name = "sales_id", length = 20, nullable = false)
    private String salesId;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'SALES_TYPE'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "type", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity type;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "nickname", length = 15)
    private String nickname;

    @Column(name = "mobile_no", length = 20)
    private String mobileNo;

    @Column(name = "bank_account_no", length = 15)
    private String bankAccountNo;

    @Column(name = "bank_name", length = 20)
    private String bankName;

    @Column(name = "bank_account_name", length = 20)
    private String bankAccountName;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'SALES_TEAM'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "team", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity team;
}
