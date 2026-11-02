package com.nutalig.entity;
import com.nutalig.constant.ShippingContainerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Entity @Table(name = "shipping_container")
public class ShippingContainerEntity extends AuditDateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private Long version;
    @Column(name = "container_job_no", length = 50, nullable = false, unique = true)
    private String containerJobNo;
    @Column(name = "container_no", length = 50)
    private String containerNo;
    @Column(name = "active_container_no", length = 50, unique = true)
    private String activeContainerNo;
    @Column(name = "seal_no", length = 100)
    private String sealNo;
    @Column(name = "tracking_no", length = 50)
    private String trackingNo;
    @Column(name = "container_size", length = 20, nullable = false)
    private String containerSize;
    @Column(name = "shipping_method", length = 50, nullable = false)
    private String shippingMethod;
    @Column(name = "origin", length = 255)
    private String origin;
    @Column(name = "destination_name_snapshot", length = 500)
    private String destinationName;
    @Column(name = "carrier_name_snapshot", length = 255)
    private String carrierName;
    @Column(name = "remark", length = 2000)
    private String remark;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_shipping_id")
    private SupplierShippingEntity supplierShipping;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destination_id")
    private SupplierShippingDestinationEntity destination;
    @Column(name = "capacity_cbm", precision = 18, scale = 6)
    private BigDecimal capacityCbm;
    @Column(name = "max_cargo_weight_kg", precision = 18, scale = 6)
    private BigDecimal maxCargoWeightKg;
    @Column(name = "planned_departure_date")
    private LocalDate plannedDepartureDate;
    @Column(name = "planned_arrival_date")
    private LocalDate plannedArrivalDate;
    @Column(name = "sealed_at")
    private ZonedDateTime sealedAt;
    @Column(name = "departed_at")
    private ZonedDateTime departedAt;
    @Column(name = "received_at")
    private ZonedDateTime receivedAt;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 30)
    private ShippingContainerStatus status = ShippingContainerStatus.DRAFT;
    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc") @org.hibernate.annotations.BatchSize(size = 50)
    private List<ShippingContainerLoadEntity> loads = new ArrayList<>();
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof ShippingContainerEntity container && getId() != null && getId().equals(container.getId());
    }
    @Override
    public int hashCode() { return ShippingContainerEntity.class.hashCode(); }

    public void addLoad(ShippingContainerLoadEntity load) { loads.add(load); load.setContainer(this); }
}
