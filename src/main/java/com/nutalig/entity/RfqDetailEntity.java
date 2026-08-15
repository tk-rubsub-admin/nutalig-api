package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "rfq_detail")
public class RfqDetailEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_header_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private RfqHeaderEntity requestPriceHeader;

    @Column(name = "option_name", length = 255)
    private String optionName;

    @Column(name = "spec", columnDefinition = "TEXT", nullable = false)
    private String spec;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "recommend", columnDefinition = "TEXT")
    private String recommend;

    @Column(name = "commission", precision = 18, scale = 4)
    private BigDecimal commission;

    @Column(name = "package_dimension", columnDefinition = "TEXT")
    private String packageDimension;

    @Column(name = "package_weight", columnDefinition = "TEXT")
    private String packageWeight;

    @Column(name = "package_capacity", columnDefinition = "TEXT")
    private String packageCapacity;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private SupplierEntity supplier;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = Boolean.FALSE;

    @Column(name = "archived_by")
    private String archivedBy;

    @Column(name = "archived_at")
    private java.time.ZonedDateTime archivedAt;

    @Column(name = "created_by")
    @ToString.Include
    private String createdBy;

    @Column(name = "updated_by")
    @ToString.Include
    private String updatedBy;

    @OneToMany(mappedBy = "requestPriceDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Where(clause = "is_archived = false")
    private List<RfqTierEntity> tiers = new ArrayList<>();

    @OneToMany(mappedBy = "requestPriceDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Where(clause = "is_archived = false")
    private List<RfqTierSplitEntity> tierSplits = new ArrayList<>();

    public void addTier(RfqTierEntity tier) {
        tiers.add(tier);
        tier.setRequestPriceDetail(this);
    }

    public void removeTier(RfqTierEntity tier) {
        tiers.remove(tier);
        tier.setRequestPriceDetail(null);
    }

    public void addTierSplit(RfqTierSplitEntity tierSplit) {
        tierSplits.add(tierSplit);
        tierSplit.setRequestPriceDetail(this);
    }

    public void removeTierSplit(RfqTierSplitEntity tierSplit) {
        tierSplits.remove(tierSplit);
        tierSplit.setRequestPriceDetail(null);
    }
}
