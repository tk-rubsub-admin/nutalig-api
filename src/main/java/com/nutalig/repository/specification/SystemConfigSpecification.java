package com.nutalig.repository.specification;

import com.nutalig.constant.SystemConstant;
import com.nutalig.entity.SystemConfigEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

public class SystemConfigSpecification {

    public static Specification<SystemConfigEntity> groupCodeEqual(SystemConstant groupCode) {
        if (groupCode == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id").get("groupCode"), groupCode);
    }

    public static Specification<SystemConfigEntity> codeEqual(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("id").get("code")), code.trim().toLowerCase());
    }

    public static Specification<SystemConfigEntity> keywordContain(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }

        String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("id").get("code")), keywordPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("nameTh")), keywordPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("nameEn")), keywordPattern)
        );
    }
}
