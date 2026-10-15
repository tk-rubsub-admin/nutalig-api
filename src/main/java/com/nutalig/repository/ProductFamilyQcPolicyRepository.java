package com.nutalig.repository;

import com.nutalig.entity.ProductFamilyQcPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nutalig.entity.id.ProductFamilyQcPolicyId;
import java.util.Optional;

public interface ProductFamilyQcPolicyRepository extends JpaRepository<ProductFamilyQcPolicyEntity, ProductFamilyQcPolicyId> {

    Optional<ProductFamilyQcPolicyEntity> findByTaxonomyRevisionAndProductFamilyCodeAndIsActiveTrue(
            String taxonomyRevision, String productFamilyCode);
}
