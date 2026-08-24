package com.nutalig.repository.specification;

import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.constant.ProcurementStatus;
import com.nutalig.constant.UrgentRequestStatus;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.SalesOrderDetailEntity;
import com.nutalig.entity.SalesOrderEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class SalesOrderSpecification {

    private SalesOrderSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<SalesOrderEntity> salesOrderNoEqual(String salesOrderNo) {
        if (StringUtils.isBlank(salesOrderNo)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("salesOrderNo"), salesOrderNo.trim());
    }

    public static Specification<SalesOrderEntity> customerIdEqual(String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<SalesOrderEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            return cb.equal(customerJoin.get("id"), customerId.trim());
        };
    }

    public static Specification<SalesOrderEntity> salesIdEqual(String salesId) {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<SalesOrderEntity, EmployeeEntity> salesJoin = root.join("sales", JoinType.LEFT);
            return cb.equal(salesJoin.get("employeeId"), salesId.trim());
        };
    }

    public static Specification<SalesOrderEntity> statusEqual(SalesOrderStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<SalesOrderEntity> statusIn(List<SalesOrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<SalesOrderEntity> urgentRequestStatusEqual(UrgentRequestStatus urgentRequestStatus) {
        if (urgentRequestStatus == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("urgentRequestStatus"), urgentRequestStatus);
    }

    public static Specification<SalesOrderEntity> procurementStatusIn(List<ProcurementStatus> procurementStatuses) {
        if (procurementStatuses == null || procurementStatuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("procurementStatus").in(procurementStatuses);
    }

    public static Specification<SalesOrderEntity> docDateBetween(LocalDate start, LocalDate end) {
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

    public static Specification<SalesOrderEntity> keywordContains(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            Join<SalesOrderEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            Join<SalesOrderEntity, SalesOrderDetailEntity> itemJoin = root.join("items", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("salesOrderNo")), likeKeyword),
                    cb.like(cb.lower(customerJoin.get("customerName")), likeKeyword),
                    cb.like(cb.lower(itemJoin.get("name")), likeKeyword)
            );
        };
    }
}
