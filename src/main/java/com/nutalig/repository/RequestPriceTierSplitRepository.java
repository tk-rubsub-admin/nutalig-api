package com.nutalig.repository;

import com.nutalig.entity.RfqTierSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceTierSplitRepository extends JpaRepository<RfqTierSplitEntity, Long> {

    List<RfqTierSplitEntity> findByRequestPriceDetailIdOrderByQuantityAscIdAsc(Long requestPriceDetailId);

    List<RfqTierSplitEntity> findByRequestPriceDetailIdAndQuantity(Long requestPriceDetailId, java.math.BigDecimal quantity);
}
