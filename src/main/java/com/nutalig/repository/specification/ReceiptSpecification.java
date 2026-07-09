package com.nutalig.repository.specification;

import com.nutalig.constant.ReceiptStatus;
import com.nutalig.constant.ReceiptType;
import com.nutalig.entity.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class ReceiptSpecification {

    private ReceiptSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<ReceiptEntity> receiptNoEqual(String receiptNo) {
        if (StringUtils.isBlank(receiptNo)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("receiptNo"), receiptNo.trim());
    }

    public static Specification<ReceiptEntity> invoiceNoEqual(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<ReceiptEntity, InvoiceEntity> invoiceJoin = root.join("invoice", JoinType.LEFT);
            return cb.equal(invoiceJoin.get("invoiceNo"), invoiceNo.trim());
        };
    }

    public static Specification<ReceiptEntity> customerIdEqual(String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<ReceiptEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            return cb.equal(customerJoin.get("id"), customerId.trim());
        };
    }

    public static Specification<ReceiptEntity> salesIdEqual(String salesId) {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<ReceiptEntity, EmployeeEntity> salesJoin = root.join("sales", JoinType.LEFT);
            return cb.equal(salesJoin.get("employeeId"), salesId.trim());
        };
    }

    public static Specification<ReceiptEntity> receiptTypeEqual(ReceiptType receiptType) {
        if (receiptType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("receiptType"), receiptType);
    }

    public static Specification<ReceiptEntity> statusEqual(ReceiptStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ReceiptEntity> statusIn(List<ReceiptStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<ReceiptEntity> docDateBetween(LocalDate start, LocalDate end) {
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

    public static Specification<ReceiptEntity> keywordContains(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            Join<ReceiptEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            Join<ReceiptEntity, ReceiptDetailEntity> itemJoin = root.join("items", JoinType.LEFT);
            Join<ReceiptEntity, InvoiceEntity> invoiceJoin = root.join("invoice", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("receiptNo")), likeKeyword),
                    cb.like(cb.lower(invoiceJoin.get("invoiceNo")), likeKeyword),
                    cb.like(cb.lower(root.get("quotationNo")), likeKeyword),
                    cb.like(cb.lower(root.get("customerNameSnapshot")), likeKeyword),
                    cb.like(cb.lower(customerJoin.get("customerName")), likeKeyword),
                    cb.like(cb.lower(itemJoin.get("name")), likeKeyword)
            );
        };
    }
}
