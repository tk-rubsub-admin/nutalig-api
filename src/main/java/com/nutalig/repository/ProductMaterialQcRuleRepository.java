package com.nutalig.repository;

import com.nutalig.entity.ProductMaterialQcRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nutalig.constant.QcTypes.RuleStatus;
import java.util.List;

public interface ProductMaterialQcRuleRepository extends JpaRepository<ProductMaterialQcRuleEntity, Long> {

    List<ProductMaterialQcRuleEntity> findAllByTaxonomyRevisionAndProductFamilyCodeAndProductMaterialCodeAndStatusOrderByIdAsc(
            String taxonomyRevision, String productFamilyCode, String productMaterialCode, RuleStatus status);
}
