package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "request_price_detail")
public class RequestPriceDetailEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_price_header_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private RequestPriceHeaderEntity requestPriceHeader;

    @Column(name = "option_name", length = 255)
    private String optionName;

    @Column(name = "spec", columnDefinition = "TEXT", nullable = false)
    private String spec;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by")
    @ToString.Include
    private String createdBy;

    @Column(name = "updated_by")
    @ToString.Include
    private String updatedBy;

    @OneToMany(mappedBy = "requestPriceDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestPriceTierEntity> tiers = new ArrayList<>();

    public void addTier(RequestPriceTierEntity tier) {
        tiers.add(tier);
        tier.setRequestPriceDetail(this);
    }

    public void removeTier(RequestPriceTierEntity tier) {
        tiers.remove(tier);
        tier.setRequestPriceDetail(null);
    }
}
