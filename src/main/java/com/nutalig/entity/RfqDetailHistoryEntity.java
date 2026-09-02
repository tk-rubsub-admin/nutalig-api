package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "rfq_detail_history")
public class RfqDetailHistoryEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(name = "rfq_id", nullable = false)
    private String rfqId;

    @Column(name = "detail_set_no", nullable = false)
    private Integer detailSetNo;

    @Column(name = "source_detail_id")
    private Long sourceDetailId;

    @Column(name = "option_name", length = 255)
    private String optionName;

    @Column(name = "plan", length = 255)
    private String plan;

    @Column(name = "spec", columnDefinition = "TEXT", nullable = false)
    private String spec;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "internal_remark", columnDefinition = "TEXT")
    private String internalRemark;

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

    @Column(name = "supplier_id")
    private String supplierId;

    @Column(name = "snapshot_json", columnDefinition = "LONGTEXT", nullable = false)
    private String snapshotJson;

    @Column(name = "archived_by")
    private String archivedBy;

    @Column(name = "archived_at")
    private ZonedDateTime archivedAt;
}
