package com.nutalig.repository.specification;

import com.nutalig.constant.InvoiceStatus;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.InvoiceDetailEntity;
import com.nutalig.entity.InvoiceEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class InvoiceSpecification {

    private InvoiceSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<InvoiceEntity> invoiceNoEqual(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("invoiceNo"), invoiceNo.trim());
    }

    public static Specification<InvoiceEntity> customerIdEqual(String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<InvoiceEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            return cb.equal(customerJoin.get("id"), customerId.trim());
        };
    }

    public static Specification<InvoiceEntity> salesIdEqual(String salesId) {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<InvoiceEntity, EmployeeEntity> salesJoin = root.join("sales", JoinType.LEFT);
            return cb.equal(salesJoin.get("employeeId"), salesId.trim());
        };
    }

    public static Specification<InvoiceEntity> statusEqual(InvoiceStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<InvoiceEntity> statusIn(List<InvoiceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<InvoiceEntity> docDateBetween(LocalDate start, LocalDate end) {
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

    public static Specification<InvoiceEntity> keywordContains(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            Join<InvoiceEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            Join<InvoiceEntity, InvoiceDetailEntity> itemJoin = root.join("items", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("invoiceNo")), likeKeyword),
                    cb.like(cb.lower(root.get("quotationNo")), likeKeyword),
                    cb.like(cb.lower(root.get("customerNameSnapshot")), likeKeyword),
                    cb.like(cb.lower(customerJoin.get("customerName")), likeKeyword),
                    cb.like(cb.lower(itemJoin.get("name")), likeKeyword)
            );
        };
    }
}
