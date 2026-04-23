package com.nutalig.repository;

import com.nutalig.entity.DistrictEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<DistrictEntity, String> {

    List<DistrictEntity> findByProvinceId(String provinceId);

    Optional<DistrictEntity> findByNameTh(String nameTh);
}
