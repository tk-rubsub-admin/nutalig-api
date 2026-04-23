package com.nutalig.repository.specification;

import com.nutalig.constant.RFQStatus;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.RequestPriceHeaderEntity;
import com.nutalig.entity.SalesEntity;
import com.nutalig.entity.SystemConfigEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

public class RequestPriceHeaderSpecification {

    public static Specification<RequestPriceHeaderEntity> idEqual(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        return (root, query, cb) -> cb.equal(cb.lower(root.get("id")), id.trim().toLowerCase());
    }

    public static Specification<RequestPriceHeaderEntity> statusEqual(RFQStatus status) {
        if (status == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<RequestPriceHeaderEntity> customerIdEqual(String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RequestPriceHeaderEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            return cb.equal(cb.lower(customerJoin.get("id")), customerId.trim().toLowerCase());
        };
    }

    public static Specification<RequestPriceHeaderEntity> salesIdEqual(String salesId) {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RequestPriceHeaderEntity, SalesEntity> salesJoin = root.join("sales", JoinType.LEFT);
            return cb.equal(cb.lower(salesJoin.get("salesId")), salesId.trim().toLowerCase());
        };
    }

    public static Specification<RequestPriceHeaderEntity> orderTypeCodeEqual(String orderTypeCode) {
        if (StringUtils.isBlank(orderTypeCode)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RequestPriceHeaderEntity, SystemConfigEntity> orderTypeJoin = root.join("orderType", JoinType.LEFT);
            return cb.equal(cb.lower(orderTypeJoin.get("id").get("code")), orderTypeCode.trim().toLowerCase());
        };
    }

    public static Specification<RequestPriceHeaderEntity> keywordContain(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }

        return (root, query, cb) -> {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Join<RequestPriceHeaderEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            Join<RequestPriceHeaderEntity, SalesEntity> salesJoin = root.join("sales", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("id")), pattern),
                    cb.like(cb.lower(root.get("contactName")), pattern),
                    cb.like(cb.lower(root.get("contactPhone")), pattern),
                    cb.like(cb.lower(root.get("productFamily")), pattern),
                    cb.like(cb.lower(root.get("productUsage")), pattern),
                    cb.like(cb.lower(root.get("systemMechanic")), pattern),
                    cb.like(cb.lower(root.get("material")), pattern),
                    cb.like(cb.lower(root.get("capacity")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(customerJoin.get("customerName")), pattern),
                    cb.like(cb.lower(customerJoin.get("companyName")), pattern),
                    cb.like(cb.lower(salesJoin.get("name")), pattern),
                    cb.like(cb.lower(salesJoin.get("nickname")), pattern)
            );
        };
    }
}
