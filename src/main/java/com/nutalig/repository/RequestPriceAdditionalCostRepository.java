package com.nutalig.repository;

import com.nutalig.entity.RfqAdditionalCostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceAdditionalCostRepository extends JpaRepository<RfqAdditionalCostEntity, Long> {

    List<RfqAdditionalCostEntity> findByRequestPriceHeaderIdOrderBySortOrderAscIdAsc(String requestPriceHeaderId);
}
