package com.nutalig.repository.specification;

import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.utils.SqlUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import static com.nutalig.constant.SystemConstant.CUSTOMER_TYPE;

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
        if (StringUtils.isNotEmpty(nameContain)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("customerName"),
                    SqlUtil.buildContainString(nameContain));
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

    public static Specification<CustomerEntity> saleAccountEqual(String saleAccountEqual) {
        if (StringUtils.isNotEmpty(saleAccountEqual)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("salesAccount"), saleAccountEqual);
        }
        return null;
    }

    public static Specification<CustomerEntity> keywordContain(String keyword) {
        if (StringUtils.isNotEmpty(keyword)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(root.get("id"), SqlUtil.buildContainString(keyword)),
                    criteriaBuilder.like(root.get("customerName"), SqlUtil.buildContainString(keyword)),
                    criteriaBuilder.like(root.get("companyName"), SqlUtil.buildContainString(keyword)),
                    criteriaBuilder.like(root.get("taxId"), SqlUtil.buildContainString(keyword)),
                    criteriaBuilder.like(root.get("salesAccount"), SqlUtil.buildContainString(keyword))
            );
        }
        return null;
    }
}
