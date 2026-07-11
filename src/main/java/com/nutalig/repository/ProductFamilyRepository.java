package com.nutalig.repository;

import com.nutalig.entity.ProductFamilyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFamilyRepository extends JpaRepository<ProductFamilyEntity, String> {
    List<ProductFamilyEntity> findAllByIsActiveTrueOrderByCodeAsc();

    Optional<ProductFamilyEntity> findByCodeAndIsActiveTrue(String code);
}
