package com.nutalig.repository.specification;

import com.nutalig.constant.QuotationStatus;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.QuotationEntity;
import jakarta.persistence.criteria.Join;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

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
                Join<QuotationEntity, CustomerEntity> quotationEntityCustomerEntityJoin = root.join("customerEntity");
                return criteriaBuilder.like(quotationEntityCustomerEntityJoin.get("id"), custId);
            };
        }
        return null;
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

}
