package com.nutalig.repository;

import com.nutalig.entity.RfqHeaderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestPriceHeaderRepository extends JpaRepository<RfqHeaderEntity, String>, JpaSpecificationExecutor<RfqHeaderEntity> {
}
