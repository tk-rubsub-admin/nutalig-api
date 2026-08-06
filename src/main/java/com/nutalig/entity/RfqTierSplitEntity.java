package com.nutalig.entity;

import com.nutalig.constant.Currency;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "rfq_tier_split",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rfq_tier_detail_qty",
                        columnNames = {"rfq_detail_id", "quantity"}
                )
        }
)
public class RfqTierSplitEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_detail_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private RfqDetailEntity requestPriceDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id")
    @ToString.Exclude
    private SupplierEntity supplier;

    @Column(name = "quantity", precision = 18, scale = 0, nullable = false)
    @ToString.Include
    private BigDecimal quantity;

    @Column(name = "sell_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal sellPrice;

    @Column(name = "commission", precision = 18, scale = 4)
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @Column(name = "land_freight_cost", precision = 18, scale = 4)
    private BigDecimal landFreightCost;

    @Column(name = "land_freight_qty", precision = 18, scale = 4)
    private BigDecimal landFreightQty;

    @Column(name = "sea_freight_qty", precision = 18, scale = 4)
    private BigDecimal seaFreightQty;

    @Column(name = "sea_freight_cost", precision = 18, scale = 4)
    private BigDecimal seaFreightCost;

    @Column(name = "is_fcl")
    private Boolean isFcl;

    @Column(name = "is_share_fcl")
    private Boolean isShareFCL;

}
