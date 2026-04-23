package com.nutalig.repository;

import com.nutalig.entity.SubDistrictEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubDistrictRepository extends JpaRepository<SubDistrictEntity, String> {

    List<SubDistrictEntity> findByDistrictId(String districtId);

    Optional<SubDistrictEntity> findByNameTh(String nameTh);

}
