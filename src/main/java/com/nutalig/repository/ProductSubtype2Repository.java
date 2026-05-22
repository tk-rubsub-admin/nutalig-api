package com.nutalig.repository;

import com.nutalig.entity.ProductSubtype2Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubtype2Repository extends JpaRepository<ProductSubtype2Entity, String> {

    List<ProductSubtype2Entity> findAllByOrderByProductSubtype1CodeAscCodeAsc();

    List<ProductSubtype2Entity> findAllByProductSubtype1CodeOrderByCodeAsc(String productSubtype1Code);

    boolean existsByProductSubtype1Code(String productSubtype1Code);

}
