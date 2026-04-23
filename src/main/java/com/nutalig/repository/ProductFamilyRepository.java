package com.nutalig.repository;

import com.nutalig.entity.ProductFamilyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductFamilyRepository extends JpaRepository<ProductFamilyEntity, String> {
}
