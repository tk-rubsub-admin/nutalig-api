package com.nutalig.repository.specification;

import com.nutalig.constant.EmployeeStatus;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.utils.SqlUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecification {

    private EmployeeSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<EmployeeEntity> employeeIdEqual(String employeeIdEqual) {
        if (StringUtils.isBlank(employeeIdEqual)) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("employeeId")), employeeIdEqual.trim().toLowerCase());
    }

    public static Specification<EmployeeEntity> nameContain(String nameContain) {
        if (StringUtils.isBlank(nameContain)) {
            return null;
        }

        String pattern = SqlUtil.buildContainString(nameContain.trim()).toLowerCase();
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstNameTh")), pattern),
                cb.like(cb.lower(root.get("lastNameTh")), pattern),
                cb.like(cb.lower(root.get("nickName")), pattern)
        );
    }

    public static Specification<EmployeeEntity> positionEqual(String positionEqual) {
        if (StringUtils.isBlank(positionEqual)) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("position").get("id").get("code")), positionEqual.trim().toLowerCase());
    }

    public static Specification<EmployeeEntity> statusEqual(EmployeeStatus statusEqual) {
        if (statusEqual == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("status"), statusEqual);
    }

    public static Specification<EmployeeEntity> keywordContain(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }

        String pattern = SqlUtil.buildContainString(keyword.trim()).toLowerCase();
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("employeeId")), pattern),
                cb.like(cb.lower(root.get("firstNameTh")), pattern),
                cb.like(cb.lower(root.get("lastNameTh")), pattern),
                cb.like(cb.lower(root.get("nickName")), pattern),
                cb.like(cb.lower(root.get("position").get("nameTh")), pattern),
                cb.like(cb.lower(root.get("position").get("nameEn")), pattern),
                cb.like(cb.lower(root.get("phoneNumber")), pattern),
                cb.like(cb.lower(root.get("additional")), pattern),
                cb.like(cb.lower(root.get("team").get("nameTh")), pattern),
                cb.like(cb.lower(root.get("team").get("nameEn")), pattern)
        );
    }
}
