package com.nutalig.entity;

import com.nutalig.constant.Status;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "customer")
@Entity(name = "Customer")
public class CustomerEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(generator = "customerIdGenerator")
    @GenericGenerator(name = "customerIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-RTC"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ToString.Include
    @Column(name = "customer_name")
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @ToString.Include
    private Status status;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'CUSTOMER_TYPE'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "customer_type", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity customerType;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'CUSTOMER_CREDIT_TERM'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "customer_credit_term", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity customerCreditTerm;

    @Column(name = "tax_id")
    @ToString.Include
    private String taxId;

    @Column(name = "company_name")
    @ToString.Include
    private String companyName;

    @Column(name = "branch_number")
    @ToString.Include
    private String branchNumber;

    @Column(name = "branch_name")
    @ToString.Include
    private String branchName;

    @Column(name = "email")
    @ToString.Include
    private String email;

    @Column(name = "sales_account")
    @ToString.Include
    private String salesAccount;

    @Column(name = "co_sales_account")
    @ToString.Include
    private String coSalesAccount;

    @Column(name = "created_by")
    @ToString.Include
    private String createdBy;

    @Column(name = "updated_by")
    @ToString.Include
    private String updatedBy;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, id DESC")
    private List<CustomerAddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerContactEntity> contacts = new ArrayList<>();

    public void addAddress(CustomerAddressEntity address) {
        addresses.add(address);
        address.setCustomer(this);
    }

    public void addContact(CustomerContactEntity contact) {
        contacts.add(contact);
        contact.setCustomer(this);
    }
}