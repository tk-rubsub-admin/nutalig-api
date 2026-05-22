package com.nutalig.repository;

import com.nutalig.entity.ProductSubtype1Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubtype1Repository extends JpaRepository<ProductSubtype1Entity, String> {

    List<ProductSubtype1Entity> findAllByOrderByProductFamilyCodeAscCodeAsc();

    List<ProductSubtype1Entity> findAllByProductFamilyCodeOrderByCodeAsc(String productFamilyCode);

    boolean existsByProductFamilyCode(String productFamilyCode);

}
