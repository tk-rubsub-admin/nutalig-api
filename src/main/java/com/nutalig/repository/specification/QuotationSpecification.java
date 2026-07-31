package com.nutalig.repository.specification;

import com.nutalig.constant.QuotationStatus;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.QuotationDetailEntity;
import com.nutalig.entity.QuotationEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class QuotationSpecification {

    public QuotationSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<QuotationEntity> docNoEqual(String docNo) {
        if (StringUtils.isNotEmpty(docNo)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("quotationNo"), docNo);
        }
        return null;
    }

    public static Specification<QuotationEntity> customerIdEqual(String custId) {
        if (StringUtils.isNotEmpty(custId)) {
            return (root, query, criteriaBuilder) -> {
                Join<QuotationEntity, CustomerEntity> quotationEntityCustomerEntityJoin = root.join("customer", JoinType.LEFT);
                return criteriaBuilder.equal(quotationEntityCustomerEntityJoin.get("id"), custId.trim());
            };
        }
        return null;
    }

    public static Specification<QuotationEntity> salesIdEqual(String salesId) {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<QuotationEntity, EmployeeEntity> salesJoin = root.join("sales", JoinType.LEFT);
            return cb.equal(salesJoin.get("employeeId"), salesId.trim());
        };
    }

    public static Specification<QuotationEntity> statusEqual(QuotationStatus status) {
        if (status != null) {
            return (root, query, cb) ->
                    cb.equal(root.get("status"), status);
        }
        return null;
    }


    public static Specification<QuotationEntity> docDateBetween(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return (root, query, cb) ->
                    cb.between(root.get("docDate"), start, end);
        }

        if (start != null) {
            return (root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("docDate"), start);
        }

        if (end != null) {
            return (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("docDate"), end);
        }

        return null;
    }

    public static Specification<QuotationEntity> keywordContains(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            Join<QuotationEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            Join<QuotationEntity, EmployeeEntity> salesJoin = root.join("sales", JoinType.LEFT);
            Join<QuotationEntity, QuotationDetailEntity> itemJoin = root.join("items", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("quotationNo")), likeKeyword),
                    cb.like(cb.lower(customerJoin.get("customerName")), likeKeyword),
                    cb.like(cb.lower(customerJoin.get("companyName")), likeKeyword),
                    cb.like(cb.lower(salesJoin.get("employeeId")), likeKeyword),
                    cb.like(cb.lower(itemJoin.get("name")), likeKeyword)
            );
        };
    }

}
