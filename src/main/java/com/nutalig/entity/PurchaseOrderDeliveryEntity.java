package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.DeliveryContainerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_delivery")
public class PurchaseOrderDeliveryEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_no", nullable = false, unique = true, length = 50)
    private String deliveryNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "expected_warehouse_date")
    private LocalDate expectedWarehouseDate;

    @Column(name = "received_at")
    private ZonedDateTime receivedAt;

    @Column(name = "shipping_method", length = 50)
    private String shippingMethod;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "in_transit_date")
    private LocalDate inTransitDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_location_id", referencedColumnName = "id")
    private SupplierShippingDestinationEntity shippingLocation;

    @Column(name = "shipping_location_name_snapshot", length = 255)
    private String shippingLocationNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderDeliveryStatus status = PurchaseOrderDeliveryStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "container_status", length = 30)
    private DeliveryContainerStatus containerStatus;

    // A delivery can be split across several containers. Cancelled allocations are excluded.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "purchase_order_delivery_container",
            joinColumns = @JoinColumn(name = "delivery_id"),
            inverseJoinColumns = @JoinColumn(name = "container_id"))
    @org.hibernate.annotations.BatchSize(size = 50)
    private java.util.Set<ShippingContainerEntity> containers = new java.util.LinkedHashSet<>();

    @PrePersist
    private void initializeContainerStatus() {
        if (requiresContainer() && containerStatus == null) containerStatus = DeliveryContainerStatus.NOT_ASSIGNED;
    }

    public boolean requiresContainer() {
        if (shippingMethod == null) return false;
        String normalized = shippingMethod.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.contains("_FCL") || normalized.equals("FCL");
    }

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "purchaseOrderDelivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<PurchaseOrderDeliveryDetailEntity> details = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrderDelivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<PurchaseOrderDeliveryScrapBoxEntity> scrapBoxes = new ArrayList<>();

    public void addScrapBox(PurchaseOrderDeliveryScrapBoxEntity box) {
        scrapBoxes.add(box);
        box.setPurchaseOrderDelivery(this);
    }

    public void addDetail(PurchaseOrderDeliveryDetailEntity detail) {
        if (detail == null) return;
        details.add(detail);
        detail.setPurchaseOrderDelivery(this);
    }

    public void removeDetail(PurchaseOrderDeliveryDetailEntity detail) {
        if (detail == null) return;
        details.remove(detail);
        detail.setPurchaseOrderDelivery(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderDeliveryEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
