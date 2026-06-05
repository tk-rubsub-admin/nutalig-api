package com.nutalig.entity;

import com.nutalig.constant.RFQStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.ArrayList;
import java.util.List;
import java.time.ZonedDateTime;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "rfq_header")
@Entity(name = "RequestPriceHeader")
public class RfqHeaderEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(generator = "rfqIdGenerator")
    @GenericGenerator(name = "rfqIdGenerator",
            parameters = {@org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-RFQ")},
            strategy = "com.nutalig.repository.jpa.IdWithMonthGenerator")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ToString.Include
    @Column(name = "requested_date")
    private ZonedDateTime requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @ToString.Include
    private RFQStatus status;

    @ToString.Include
    @Column(name = "contact_name")
    private String contactName;

    @ToString.Include
    @Column(name = "contact_phone")
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sales_id", referencedColumnName = "employee_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private EmployeeEntity sales;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private CustomerEntity customer;

    @OneToMany(mappedBy = "requestPriceHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqPicturesEntity> pictures = new ArrayList<>();

    @OneToMany(mappedBy = "requestPriceHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqDetailEntity> details = new ArrayList<>();

    @OneToMany(mappedBy = "requestPriceHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqAdditionalCostEntity> additionalCosts = new ArrayList<>();

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'ORDER_TYPE'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "order_type", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity orderType;

    @ToString.Exclude
    @Column(name = "product_family")
    private String productFamily;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "product_family", referencedColumnName = "code", insertable = false, updatable = false)
    private ProductFamilyEntity productFamilyEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "product_usage", referencedColumnName = "code")
    @NotFound(action = NotFoundAction.IGNORE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductSubtype1Entity productUsage;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "system_mechanic", referencedColumnName = "code")
    @NotFound(action = NotFoundAction.IGNORE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductSubtype2Entity systemMechanic;

    @Column(name = "material")
    private String materialCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns({
            @JoinColumn(name = "material", referencedColumnName = "code", insertable = false, updatable = false),
            @JoinColumn(name = "product_family", referencedColumnName = "product_family_code", insertable = false, updatable = false)
    })
    @NotFound(action = NotFoundAction.IGNORE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductMaterialEntity material;

    @ToString.Include
    @Column(name = "capacity")
    private String capacity;

    @ToString.Include
    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    @ToString.Include
    private String createdBy;

    @Column(name = "updated_by")
    @ToString.Include
    private String updatedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "procurement_id", referencedColumnName = "employee_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private EmployeeEntity procurement;

    @ToString.Include
    @Column(name = "sla_date")
    private ZonedDateTime slaDate;

    @ToString.Include
    @Column(name = "quoted_date")
    private ZonedDateTime quotedDate;

    public void addPicture(RfqPicturesEntity picture) {
        pictures.add(picture);
        picture.setRequestPriceHeader(this);
    }

    public void removePicture(RfqPicturesEntity picture) {
        pictures.remove(picture);
        picture.setRequestPriceHeader(null);
    }

    public void addDetail(RfqDetailEntity detail) {
        details.add(detail);
        detail.setRequestPriceHeader(this);
    }

    public void removeDetail(RfqDetailEntity detail) {
        details.remove(detail);
        detail.setRequestPriceHeader(null);
    }

    public void addAdditionalCost(RfqAdditionalCostEntity additionalCost) {
        additionalCosts.add(additionalCost);
        additionalCost.setRequestPriceHeader(this);
    }

    public void removeAdditionalCost(RfqAdditionalCostEntity additionalCost) {
        additionalCosts.remove(additionalCost);
        additionalCost.setRequestPriceHeader(null);
    }
}
