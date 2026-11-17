package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderDeliveryAttachmentSection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_delivery_attachment")
public class PurchaseOrderDeliveryAttachmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private PurchaseOrderDeliveryEntity delivery;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_section", nullable = false, length = 30)
    private PurchaseOrderDeliveryAttachmentSection deliverySection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "id", nullable = false)
    private UserEntity uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private ZonedDateTime uploadedAt;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderDeliveryAttachmentEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
