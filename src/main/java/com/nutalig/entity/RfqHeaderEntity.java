package com.nutalig.entity;

import com.nutalig.constant.RfqStatus;
import com.nutalig.constant.RequestInfoTo;
import com.nutalig.constant.UrgentRequestStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private RfqStatus status;

    @ToString.Include
    @Column(name = "contact_name")
    private String contactName;

    @ToString.Include
    @Column(name = "contact_phone")
    private String contactPhone;

    @ToString.Include
    @Column(name = "contact_channel")
    private String contactChannel;

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

    @Column(name = "reference_rfq_id")
    private String referenceRfqId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reference_rfq_id", referencedColumnName = "id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private RfqHeaderEntity referenceRfq;

    @OneToMany(mappedBy = "requestPriceHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqPicturesEntity> pictures = new ArrayList<>();

    @OneToMany(mappedBy = "requestPriceHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @Where(clause = "is_archived = false")
    private List<RfqDetailEntity> details = new ArrayList<>();

    @OneToMany(mappedBy = "requestPriceHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqAdditionalCostEntity> additionalCosts = new ArrayList<>();

    @OneToMany(mappedBy = "rfq")
    @jakarta.persistence.OrderBy("createdDate DESC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<QuotationEntity> quotations = new ArrayList<>();

    @OneToMany(mappedBy = "rfqHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RfqStatusTimelineEntity> statusTimelines = new ArrayList<>();

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula= @JoinFormula(value="'RFQ_TYPE'", referencedColumnName="group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "rfq_type", referencedColumnName ="code"))
    })
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private SystemConfigEntity rfqType;

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

    @Column(name = "target_price", precision = 18, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "requested_moq", columnDefinition = "TEXT")
    private String requestedMoq;

    @Column(name = "is_request_sample")
    private Boolean requestSample;

    @Column(name = "is_urgent_request")
    private Boolean urgentRequest;

    @Column(name = "urgent_request_reason", columnDefinition = "TEXT")
    private String urgentRequestReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgent_request_status", length = 30)
    private UrgentRequestStatus urgentRequestStatus;

    @Column(name = "urgent_requested_by")
    private String urgentRequestedBy;

    @Column(name = "urgent_requested_date")
    private ZonedDateTime urgentRequestedDate;

    @Column(name = "urgent_approved_by")
    private String urgentApprovedBy;

    @Column(name = "urgent_approved_date")
    private ZonedDateTime urgentApprovedDate;

    @Column(name = "urgent_rejected_by")
    private String urgentRejectedBy;

    @Column(name = "urgent_rejected_date")
    private ZonedDateTime urgentRejectedDate;

    @Column(name = "urgent_reject_reason", columnDefinition = "TEXT")
    private String urgentRejectReason;

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

    @Column(name = "accept_work_duration_minutes")
    private Long acceptWorkDurationMinutes;

    @ToString.Include
    @Column(name = "quoted_date")
    private ZonedDateTime quotedDate;

    @ToString.Include
    @Column(name = "quotation_no", length = 50)
    private String quotationNo;

    @ToString.Include
    @Column(name = "sale_order_id", length = 100)
    private String saleOrderId;

    @Column(name = "shipping_method", length = 20)
    private String shippingMethod;

    @Column(name = "request_information", columnDefinition = "TEXT")
    private String requestInformation;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_to", length = 30)
    private RequestInfoTo requestTo;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "confirmed_detail_id")
    private Long confirmedDetailId;

    @Column(name = "confirmed_tier_id")
    private Long confirmedTierId;

    @Column(name = "confirmed_supplier_quote_id", length = 255)
    private String confirmedSupplierQuoteId;

    @Column(name = "confirmed_shipping_method", length = 50)
    private String confirmedShippingMethod;

    @Column(name = "confirmed_price", precision = 18, scale = 4)
    private java.math.BigDecimal confirmedPrice;

    @ToString.Include
    @Column(name = "confirmed_date")
    private ZonedDateTime confirmedDate;

    @Column(name = "is_accept")
    private Boolean isAccept;

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

    public void addStatusTimeline(RfqStatusTimelineEntity statusTimeline) {
        statusTimelines.add(statusTimeline);
        statusTimeline.setRfqHeader(this);
    }

    public void removeStatusTimeline(RfqStatusTimelineEntity statusTimeline) {
        statusTimelines.remove(statusTimeline);
        statusTimeline.setRfqHeader(null);
    }
}
