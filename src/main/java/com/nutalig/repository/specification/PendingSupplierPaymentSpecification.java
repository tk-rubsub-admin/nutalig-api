package com.nutalig.repository.specification;

import com.nutalig.constant.PurchaseOrderPaymentScheduleStatus;
import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.controller.purchaseorder.request.SearchPendingSupplierPaymentRequest;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.entity.SupplierEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PendingSupplierPaymentSpecification {

    private PendingSupplierPaymentSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<PurchaseOrderPaymentScheduleEntity> search(
            SearchPendingSupplierPaymentRequest request
    ) {
        return (root, query, cb) -> {
            Join<PurchaseOrderPaymentScheduleEntity, PurchaseOrderEntity> purchaseOrder =
                    root.join("purchaseOrder", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), PurchaseOrderPaymentScheduleStatus.AWAITING_PAYMENT_SETUP));
            predicates.add(purchaseOrder.get("status").in(PurchaseOrderStatus.CANCELLED, PurchaseOrderStatus.CLOSED)
                    .not());

            if (request != null && StringUtils.isNotBlank(request.getSupplierId())) {
                predicates.add(cb.equal(purchaseOrder.join("supplier", JoinType.INNER).get("id"),
                        request.getSupplierId().trim()));
            }
            if (request != null && request.getDocDateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(purchaseOrder.get("docDate"), request.getDocDateStart()));
            }
            if (request != null && request.getDocDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(purchaseOrder.get("docDate"), request.getDocDateEnd()));
            }

            if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
                String keyword = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Join<PurchaseOrderEntity, SupplierEntity> supplier = purchaseOrder.join("supplier", JoinType.LEFT);
                Join<PurchaseOrderEntity, ?> salesOrder = purchaseOrder.join("salesOrder", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(purchaseOrder.get("purchaseOrderNo")), keyword),
                        cb.like(cb.lower(purchaseOrder.get("supplierNameSnapshot")), keyword),
                        cb.like(cb.lower(supplier.get("supplierName")), keyword),
                        cb.like(cb.lower(salesOrder.get("salesOrderNo")), keyword)
                ));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
