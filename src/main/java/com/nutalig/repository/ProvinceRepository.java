package com.nutalig.repository;

import com.nutalig.entity.ProvinceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<ProvinceEntity, String> {

    Optional<ProvinceEntity> findByNameTh(String s);
}
