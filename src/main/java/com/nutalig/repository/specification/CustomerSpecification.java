package com.nutalig.repository.specification;

import com.nutalig.constant.Status;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.utils.SqlUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import static com.nutalig.constant.SystemConstant.*;

public class CustomerSpecification {

    private CustomerSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<CustomerEntity> idEqual(String idEqual) {
        if (StringUtils.isNotEmpty(idEqual)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), idEqual);
        }
        return null;
    }

    public static Specification<CustomerEntity> customerNameContain(String nameContain) {
        if (StringUtils.isNotBlank(nameContain)) {
            String pattern = SqlUtil.buildContainString(nameContain.trim()).toLowerCase();
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("customerName")),
                    pattern
            );
        }
        return null;
    }

    public static Specification<CustomerEntity> customerTypeEqual(String typeEqual) {
        if (StringUtils.isNotEmpty(typeEqual)) {
            return (root, query, criteriaBuilder) -> {
                Join<CustomerEntity, SystemConfigEntity> customerEntitySystemConfigEntityJoin = root.join("customerType");
                Predicate groupCodePredicate = criteriaBuilder.equal(customerEntitySystemConfigEntityJoin.get("id").get("groupCode"), CUSTOMER_TYPE.name());
                Predicate codePredicate = criteriaBuilder.equal(customerEntitySystemConfigEntityJoin.get("id").get("code"), typeEqual);
                return criteriaBuilder.and(groupCodePredicate, codePredicate);
            };
        }
        return null;
    }

    public static Specification<CustomerEntity> customerTierEqual(String tierEqual) {
        if (StringUtils.isNotEmpty(tierEqual)) {
            return (root, query, criteriaBuilder) -> {
                Join<CustomerEntity, SystemConfigEntity> customerEntitySystemConfigEntityJoin = root.join("customerTier");
                Predicate groupCodePredicate = criteriaBuilder.equal(customerEntitySystemConfigEntityJoin.get("id").get("groupCode"), CUSTOMER_TIER.name());
                Predicate codePredicate = criteriaBuilder.equal(customerEntitySystemConfigEntityJoin.get("id").get("code"), tierEqual);
                return criteriaBuilder.and(groupCodePredicate, codePredicate);
            };
        }
        return null;
    }

    public static Specification<CustomerEntity> customerSegmentEqual(String segmentEqual) {
        if (StringUtils.isNotEmpty(segmentEqual)) {
            return (root, query, criteriaBuilder) -> {
                Join<CustomerEntity, SystemConfigEntity> customerEntitySystemConfigEntityJoin = root.join("customerSegment");
                Predicate groupCodePredicate = criteriaBuilder.equal(customerEntitySystemConfigEntityJoin.get("id").get("groupCode"), CUSTOMER_SEGMENT.name());
                Predicate codePredicate = criteriaBuilder.equal(customerEntitySystemConfigEntityJoin.get("id").get("code"), segmentEqual);
                return criteriaBuilder.and(groupCodePredicate, codePredicate);
            };
        }
        return null;
    }

    public static Specification<CustomerEntity> saleAccountEqual(String saleAccountEqual) {
        if (StringUtils.isNotEmpty(saleAccountEqual)) {
            return (root, query, criteriaBuilder) -> {
                query.distinct(true);
                Join<CustomerEntity, String> salesAccountsJoin = root.join("salesAccounts", JoinType.LEFT);
                return criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("salesAccount"), saleAccountEqual),
                        criteriaBuilder.equal(salesAccountsJoin, saleAccountEqual)
                );
            };
        }
        return null;
    }

    public static Specification<CustomerEntity> keywordContain(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            String containKeyword = SqlUtil.buildContainString(keyword.trim()).toLowerCase();
            return (root, query, criteriaBuilder) -> {
                query.distinct(true);
                Join<CustomerEntity, String> salesAccountsJoin = root.join("salesAccounts", JoinType.LEFT);
                return criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("id")), containKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerName")), containKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), containKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("taxId")), containKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("salesAccount")), containKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(salesAccountsJoin), containKeyword)
                );
            };
        }
        return null;
    }

    public static Specification<CustomerEntity> statusEqual(Status status) {
        if (status != null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
        }
        return null;
    }
}
