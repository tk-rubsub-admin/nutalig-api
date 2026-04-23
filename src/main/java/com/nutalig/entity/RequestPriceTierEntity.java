package com.nutalig.entity;

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
        name = "request_price_tier",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_request_price_tier_detail_qty",
                        columnNames = {"request_price_detail_id", "quantity"}
                )
        }
)
public class RequestPriceTierEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_price_detail_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private RequestPriceDetailEntity requestPriceDetail;

    @Column(name = "quantity", precision = 18, scale = 0, nullable = false)
    @ToString.Include
    private BigDecimal quantity;

    @Column(name = "product_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal productPrice;

    @Column(name = "land_freight_cost", precision = 18, scale = 4)
    private BigDecimal landFreightCost;

    @Column(name = "sea_freight_cost", precision = 18, scale = 4)
    private BigDecimal seaFreightCost;

    @Column(name = "land_total_price", precision = 18, scale = 4)
    private BigDecimal landTotalPrice;

    @Column(name = "sea_total_price", precision = 18, scale = 4)
    private BigDecimal seaTotalPrice;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
