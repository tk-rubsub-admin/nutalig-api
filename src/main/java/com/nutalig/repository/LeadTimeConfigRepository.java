package com.nutalig.repository;

import com.nutalig.entity.LeadTimeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadTimeConfigRepository extends JpaRepository<LeadTimeConfigEntity, String> {

    List<LeadTimeConfigEntity> findAllByIsActiveTrueOrderByTypeAscNameThAsc();

    List<LeadTimeConfigEntity> findAllByTypeOrderByNameThAsc(String type);

    Optional<LeadTimeConfigEntity> findByCodeAndIsActiveTrue(String code);
}
