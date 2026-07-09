package com.nutalig.repository;

import com.nutalig.entity.RfqHeaderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestPriceHeaderRepository extends JpaRepository<RfqHeaderEntity, String>, JpaSpecificationExecutor<RfqHeaderEntity> {
    Optional<RfqHeaderEntity> findFirstBySaleOrderId(String saleOrderId);
}
