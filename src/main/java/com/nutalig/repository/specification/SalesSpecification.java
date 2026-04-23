package com.nutalig.repository.specification;

import com.nutalig.entity.SalesEntity;
import com.nutalig.utils.SqlUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

public class SalesSpecification {

    private SalesSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<SalesEntity> salesIdEqual(String salesIdEqual) {
        if (StringUtils.isNotBlank(salesIdEqual)) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("salesId")), salesIdEqual.trim().toLowerCase());
        }
        return null;
    }

    public static Specification<SalesEntity> typeEqual(String typeEqual) {
        if (StringUtils.isNotBlank(typeEqual)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("type").get("id").get("code")),
                    typeEqual.trim().toLowerCase()
            );
        }
        return null;
    }

    public static Specification<SalesEntity> keywordContain(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            String pattern = SqlUtil.buildContainString(keyword.trim()).toLowerCase();
            return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("salesId")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nickname")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("mobileNo")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("type").get("nameTh")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("type").get("nameEn")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("team").get("nameTh")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("team").get("nameEn")), pattern)
            );
        }
        return null;
    }
}
