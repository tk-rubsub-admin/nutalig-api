package com.nutalig.repository;

import com.nutalig.entity.RfqTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceTierRepository extends JpaRepository<RfqTierEntity, Long> {

    List<RfqTierEntity> findByRequestPriceDetailIdOrderBySortOrderAscIdAsc(Long requestPriceDetailId);
}
