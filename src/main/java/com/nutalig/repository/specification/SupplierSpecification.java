package com.nutalig.repository.specification;

import com.nutalig.constant.Status;
import com.nutalig.entity.ProductMaterialEntity;
import com.nutalig.entity.SupplierCapabilityEntity;
import com.nutalig.entity.SupplierContactEntity;
import com.nutalig.entity.SupplierEntity;
import jakarta.persistence.criteria.Join;
import com.nutalig.utils.SqlUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

public class SupplierSpecification {

    private SupplierSpecification() {
        throw new IllegalStateException("Don't initialize this class");
    }

    public static Specification<SupplierEntity> idEqual(String idEqual) {
        if (StringUtils.isNotBlank(idEqual)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), idEqual.trim());
        }
        return null;
    }

    public static Specification<SupplierEntity> supplierNameContain(String supplierNameContain) {
        if (StringUtils.isNotBlank(supplierNameContain)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                    root.get("supplierName"),
                    SqlUtil.buildContainString(supplierNameContain.trim())
            );
        }
        return null;
    }

    public static Specification<SupplierEntity> supplierCodeEqual(String supplierCodeEqual) {
        if (StringUtils.isNotBlank(supplierCodeEqual)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    root.get("supplierCode"),
                    supplierCodeEqual.trim()
            );
        }
        return null;
    }

    public static Specification<SupplierEntity> supplierEmailContain(String supplierEmailContain) {
        if (StringUtils.isNotBlank(supplierEmailContain)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                    root.get("supplierEmail"),
                    SqlUtil.buildContainString(supplierEmailContain.trim())
            );
        }
        return null;
    }

    public static Specification<SupplierEntity> statusEqual(Status statusEqual) {
        if (statusEqual != null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), statusEqual);
        }
        return null;
    }

    public static Specification<SupplierEntity> countryCodeEqual(String countryCodeEqual) {
        if (StringUtils.isNotBlank(countryCodeEqual)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("countryCode"), countryCodeEqual.trim());
        }
        return null;
    }

    public static Specification<SupplierEntity> contactNameContain(String contactNameContain) {
        if (StringUtils.isNotBlank(contactNameContain)) {
            return (root, query, criteriaBuilder) -> {
                query.distinct(true);
                Join<SupplierEntity, SupplierContactEntity> contactJoin = root.join("contacts");
                return criteriaBuilder.like(contactJoin.get("contactName"), SqlUtil.buildContainString(contactNameContain.trim()));
            };
        }
        return null;
    }

    public static Specification<SupplierEntity> contactNumberContain(String contactNumberContain) {
        if (StringUtils.isNotBlank(contactNumberContain)) {
            return (root, query, criteriaBuilder) -> {
                query.distinct(true);
                Join<SupplierEntity, SupplierContactEntity> contactJoin = root.join("contacts");
                return criteriaBuilder.like(contactJoin.get("contactNumber"), SqlUtil.buildContainString(contactNumberContain.trim()));
            };
        }
        return null;
    }

    public static Specification<SupplierEntity> keywordContain(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            String containKeyword = SqlUtil.buildContainString(keyword.trim());
            return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(root.get("id"), containKeyword),
                    criteriaBuilder.like(root.get("supplierName"), containKeyword),
                    criteriaBuilder.like(root.get("supplierCode"), containKeyword),
                    criteriaBuilder.like(root.get("supplierEmail"), containKeyword),
                    criteriaBuilder.like(root.get("countryCode"), containKeyword),
                    criteriaBuilder.like(root.get("fullAddress"), containKeyword),
                    criteriaBuilder.like(root.get("province"), containKeyword),
                    criteriaBuilder.like(root.get("city"), containKeyword),
                    criteriaBuilder.like(root.get("district"), containKeyword),
                    criteriaBuilder.like(root.get("postalCode"), containKeyword)
            );
        }
        return null;
    }

    public static Specification<SupplierEntity> capabilityProductFamilyCodeEqual(String productFamilyCodeEqual) {
        if (StringUtils.isNotBlank(productFamilyCodeEqual)) {
            return (root, query, criteriaBuilder) -> {
                query.distinct(true);
                Join<SupplierEntity, SupplierCapabilityEntity> capabilityJoin = root.join("capabilities");
                return criteriaBuilder.and(
                        criteriaBuilder.equal(capabilityJoin.get("status"), Status.ACTIVE),
                        criteriaBuilder.equal(capabilityJoin.get("productFamilyCode"), productFamilyCodeEqual.trim())
                );
            };
        }
        return null;
    }

    public static Specification<SupplierEntity> capabilityProductMaterialCodeEqual(
            String productFamilyCodeEqual,
            String productMaterialCodeEqual
    ) {
        if (StringUtils.isNotBlank(productMaterialCodeEqual)) {
            return (root, query, criteriaBuilder) -> {
                query.distinct(true);
                Join<SupplierEntity, SupplierCapabilityEntity> capabilityJoin = root.join("capabilities");

                var directMaterialMatch = criteriaBuilder.and(
                        criteriaBuilder.equal(capabilityJoin.get("status"), Status.ACTIVE),
                        criteriaBuilder.equal(capabilityJoin.get("productMaterialCode"), productMaterialCodeEqual.trim())
                );

                if (StringUtils.isNotBlank(productFamilyCodeEqual)) {
                    directMaterialMatch = criteriaBuilder.and(
                            directMaterialMatch,
                            criteriaBuilder.equal(capabilityJoin.get("productFamilyCode"), productFamilyCodeEqual.trim())
                    );
                }

                var familyLevelMatch = criteriaBuilder.and(
                        criteriaBuilder.equal(capabilityJoin.get("status"), Status.ACTIVE),
                        criteriaBuilder.isNull(capabilityJoin.get("productMaterialCode"))
                );

                if (StringUtils.isNotBlank(productFamilyCodeEqual)) {
                    familyLevelMatch = criteriaBuilder.and(
                            familyLevelMatch,
                            criteriaBuilder.equal(capabilityJoin.get("productFamilyCode"), productFamilyCodeEqual.trim())
                    );
                } else {
                    var subquery = query.subquery(Long.class);
                    var productMaterialRoot = subquery.from(ProductMaterialEntity.class);
                    subquery.select(criteriaBuilder.literal(1L));
                    subquery.where(
                            criteriaBuilder.equal(productMaterialRoot.get("code"), productMaterialCodeEqual.trim()),
                            criteriaBuilder.equal(
                                    productMaterialRoot.get("productFamilyCode"),
                                    capabilityJoin.get("productFamilyCode")
                            )
                    );
                    familyLevelMatch = criteriaBuilder.and(familyLevelMatch, criteriaBuilder.exists(subquery));
                }

                return criteriaBuilder.or(directMaterialMatch, familyLevelMatch);
            };
        }
        return null;
    }
}
