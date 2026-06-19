package com.nutalig.repository.specification;

import com.nutalig.constant.RfqStatus;
import com.nutalig.entity.*;
import com.nutalig.utils.DateUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class RequestPriceHeaderSpecification {

    public static Specification<RfqHeaderEntity> idEqual(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        return (root, query, cb) -> cb.equal(cb.lower(root.get("id")), id.trim().toLowerCase());
    }

    public static Specification<RfqHeaderEntity> statusEqual(RfqStatus status) {
        if (status == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<RfqHeaderEntity> statusIn(List<RfqStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }

        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<RfqHeaderEntity> customerIdEqual(String customerId) {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RfqHeaderEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            return cb.equal(cb.lower(customerJoin.get("id")), customerId.trim().toLowerCase());
        };
    }

    public static Specification<RfqHeaderEntity> salesIdEqual(String salesId) {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RfqHeaderEntity, SalesEntity> salesJoin = root.join("sales", JoinType.LEFT);
            return cb.equal(cb.lower(salesJoin.get("id")), salesId.trim().toLowerCase());
        };
    }

    public static Specification<RfqHeaderEntity> procurementIdEqual(String procurementId) {
        if (StringUtils.isBlank(procurementId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RfqHeaderEntity, EmployeeEntity> procurementJoin = root.join("procurement", JoinType.LEFT);
            return cb.equal(cb.lower(procurementJoin.get("id")), procurementId.trim().toLowerCase());
        };
    }

    public static Specification<RfqHeaderEntity> orderTypeCodeEqual(String orderTypeCode) {
        if (StringUtils.isBlank(orderTypeCode)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<RfqHeaderEntity, SystemConfigEntity> orderTypeJoin = root.join("orderType", JoinType.LEFT);
            return cb.equal(cb.lower(orderTypeJoin.get("id").get("code")), orderTypeCode.trim().toLowerCase());
        };
    }

    public static Specification<RfqHeaderEntity> productFamilyEqual(String productFamily) {
        if (StringUtils.isBlank(productFamily)) {
            return null;
        }

        return (root, query, cb) -> cb.equal(cb.lower(root.get("productFamily")), productFamily.trim().toLowerCase());
    }

    public static Specification<RfqHeaderEntity> requestedDateBetween(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return null;
        }

        return (root, query, cb) -> {
            ZonedDateTime startDateTime = start == null
                    ? null
                    : start.atStartOfDay(DateUtil.getTimeZone()).withZoneSameInstant(DateUtil.getTimeZone());
            ZonedDateTime endDateTime = end == null
                    ? null
                    : end.plusDays(1).atStartOfDay(DateUtil.getTimeZone()).minusNanos(1)
                    .withZoneSameInstant(DateUtil.getTimeZone());

            if (startDateTime != null && endDateTime != null) {
                return cb.between(root.get("requestedDate"), startDateTime, endDateTime);
            }

            if (startDateTime != null) {
                return cb.greaterThanOrEqualTo(root.get("requestedDate"), startDateTime);
            }

            return cb.lessThanOrEqualTo(root.get("requestedDate"), endDateTime);
        };
    }

    public static Specification<RfqHeaderEntity> keywordContain(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }

        return (root, query, cb) -> {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Join<RfqHeaderEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
            Join<RfqHeaderEntity, SalesEntity> salesJoin = root.join("sales", JoinType.LEFT);
            Join<RfqHeaderEntity, ProductSubtype1Entity> subtype1Join = root.join("productUsage", JoinType.LEFT);
            Join<RfqHeaderEntity, ProductSubtype2Entity> subtype2Join = root.join("systemMechanic", JoinType.LEFT);
            Join<RfqHeaderEntity, ProductMaterialEntity> materialJoin = root.join("material", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("id")), pattern),
                    cb.like(cb.lower(root.get("contactName")), pattern),
                    cb.like(cb.lower(root.get("contactPhone")), pattern),
                    cb.like(cb.lower(root.get("productFamily")), pattern),
                    cb.like(cb.lower(subtype1Join.get("code")), pattern),
                    cb.like(cb.lower(subtype1Join.get("nameTh")), pattern),
                    cb.like(cb.lower(subtype1Join.get("nameEn")), pattern),
                    cb.like(cb.lower(subtype2Join.get("code")), pattern),
                    cb.like(cb.lower(subtype2Join.get("nameTh")), pattern),
                    cb.like(cb.lower(subtype2Join.get("nameEn")), pattern),
                    cb.like(cb.lower(materialJoin.get("code")), pattern),
                    cb.like(cb.lower(materialJoin.get("nameTh")), pattern),
                    cb.like(cb.lower(materialJoin.get("nameEn")), pattern),
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
