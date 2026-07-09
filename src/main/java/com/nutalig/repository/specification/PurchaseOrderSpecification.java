package com.nutalig.repository.specification;

import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.entity.PurchaseOrderDetailEntity;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.SupplierEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderSpecification {

    private PurchaseOrderSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<PurchaseOrderEntity> purchaseOrderNoEqual(String purchaseOrderNo) {
        if (StringUtils.isBlank(purchaseOrderNo)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("purchaseOrderNo"), purchaseOrderNo.trim());
    }

    public static Specification<PurchaseOrderEntity> salesOrderNoEqual(String salesOrderNo) {
        if (StringUtils.isBlank(salesOrderNo)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.join("salesOrder", JoinType.LEFT).get("salesOrderNo"), salesOrderNo.trim());
    }

    public static Specification<PurchaseOrderEntity> supplierIdEqual(String supplierId) {
        if (StringUtils.isBlank(supplierId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<PurchaseOrderEntity, SupplierEntity> supplierJoin = root.join("supplier", JoinType.LEFT);
            return cb.equal(supplierJoin.get("id"), supplierId.trim());
        };
    }

    public static Specification<PurchaseOrderEntity> statusEqual(PurchaseOrderStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseOrderEntity> statusIn(List<PurchaseOrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<PurchaseOrderEntity> docDateBetween(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return (root, query, cb) -> cb.between(root.get("docDate"), start, end);
        }
        if (start != null) {
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("docDate"), start);
        }
        if (end != null) {
            return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("docDate"), end);
        }
        return null;
    }

    public static Specification<PurchaseOrderEntity> keywordContains(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            Join<PurchaseOrderEntity, SupplierEntity> supplierJoin = root.join("supplier", JoinType.LEFT);
            Join<PurchaseOrderEntity, PurchaseOrderDetailEntity> itemJoin = root.join("items", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("purchaseOrderNo")), likeKeyword),
                    cb.like(cb.lower(root.join("salesOrder", JoinType.LEFT).get("salesOrderNo")), likeKeyword),
                    cb.like(cb.lower(root.get("supplierNameSnapshot")), likeKeyword),
                    cb.like(cb.lower(supplierJoin.get("supplierName")), likeKeyword),
                    cb.like(cb.lower(itemJoin.get("name")), likeKeyword)
            );
        };
    }
}
