package com.nutalig.repository.specification;

import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderDeliveryRequest;
import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import com.nutalig.utils.DateUtil;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.Locale;

public final class PurchaseOrderDeliverySpecification {
    private PurchaseOrderDeliverySpecification() {}

    public static Specification<PurchaseOrderDeliveryEntity> search(SearchPurchaseOrderDeliveryRequest request) {
        return (root, query, cb) -> {
            var po = root.join("purchaseOrder");
            var supplier = po.join("supplier", JoinType.LEFT);
            var salesOrder = po.join("salesOrder", JoinType.LEFT);
            var filters = new ArrayList<Predicate>();
            if (StringUtils.isNotBlank(request.getDeliveryNo()))
                filters.add(cb.like(cb.lower(root.get("deliveryNo")), contains(request.getDeliveryNo()), '\\'));
            if (StringUtils.isNotBlank(request.getPurchaseOrderNo()))
                filters.add(cb.like(cb.lower(po.get("purchaseOrderNo")), contains(request.getPurchaseOrderNo()), '\\'));
            if (StringUtils.isNotBlank(request.getSalesOrderNo()))
                filters.add(cb.like(cb.lower(salesOrder.get("salesOrderNo")), contains(request.getSalesOrderNo()), '\\'));
            if (StringUtils.isNotBlank(request.getSupplierId()))
                filters.add(cb.equal(supplier.get("id"), request.getSupplierId().trim()));
            if (StringUtils.isNotBlank(request.getSupplierName())) {
                String pattern = contains(request.getSupplierName());
                filters.add(cb.or(cb.like(cb.lower(po.get("supplierNameSnapshot")), pattern, '\\'),
                        cb.like(cb.lower(supplier.get("supplierName")), pattern, '\\')));
            }
            if (request.getSupplierShippingId() != null) filters.add(cb.equal(po.get("supplierShipping").get("id"),request.getSupplierShippingId()));
            if (request.getShippingLocationId() != null) filters.add(cb.equal(root.get("shippingLocation").get("id"),request.getShippingLocationId()));
            if (request.getContainerStatuses() != null && !request.getContainerStatuses().isEmpty())
                filters.add(root.get("containerStatus").in(request.getContainerStatuses()));
            else if (request.getContainerStatus() != null)
                filters.add(cb.equal(root.get("containerStatus"), request.getContainerStatus()));
            if (request.getStatus() != null) filters.add(cb.equal(root.get("status"), request.getStatus()));
            if (StringUtils.isNotBlank(request.getShippingMethod())) {
                String method = request.getShippingMethod().trim().toUpperCase(Locale.ROOT);
                filters.add("SEA".equals(method)
                        ? cb.like(cb.upper(root.get("shippingMethod")), "SEA%")
                        : cb.equal(cb.upper(root.get("shippingMethod")), method));
            }
            if (request.getShippedDateStart() != null)
                filters.add(cb.greaterThanOrEqualTo(root.get("shippedAt"), request.getShippedDateStart().atStartOfDay(DateUtil.getTimeZone())));
            if (request.getShippedDateEnd() != null)
                filters.add(cb.lessThan(root.get("shippedAt"), request.getShippedDateEnd().plusDays(1).atStartOfDay(DateUtil.getTimeZone())));
            if (request.getExpectedWarehouseDateStart() != null)
                filters.add(cb.greaterThanOrEqualTo(root.get("expectedWarehouseDate"), request.getExpectedWarehouseDateStart()));
            if (request.getExpectedWarehouseDateEnd() != null)
                filters.add(cb.lessThanOrEqualTo(root.get("expectedWarehouseDate"), request.getExpectedWarehouseDateEnd()));
            if (StringUtils.isNotBlank(request.getKeyword())) {
                String pattern = contains(request.getKeyword());
                filters.add(cb.or(
                        cb.like(cb.lower(root.get("deliveryNo")), pattern, '\\'),
                        cb.like(cb.lower(po.get("purchaseOrderNo")), pattern, '\\'),
                        cb.like(cb.lower(salesOrder.get("salesOrderNo")), pattern, '\\'),
                        cb.like(cb.lower(po.get("supplierNameSnapshot")), pattern, '\\'),
                        cb.like(cb.lower(supplier.get("supplierName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("trackingNumber")), pattern, '\\'),
                        cb.like(cb.lower(root.get("shippingLocationNameSnapshot")), pattern, '\\')));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
