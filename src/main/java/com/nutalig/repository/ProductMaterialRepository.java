package com.nutalig.repository;

import com.nutalig.entity.ProductMaterialEntity;
import com.nutalig.entity.id.ProductMaterialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMaterialRepository extends JpaRepository<ProductMaterialEntity, ProductMaterialId> {

    List<ProductMaterialEntity> findAllByProductFamilyCodeOrderByCodeAsc(String productFamilyCode);

}
