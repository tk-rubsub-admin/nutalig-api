package com.nutalig.repository;

import com.nutalig.entity.RequestPriceAdditionalCostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceAdditionalCostRepository extends JpaRepository<RequestPriceAdditionalCostEntity, Long> {

    List<RequestPriceAdditionalCostEntity> findByRequestPriceHeaderIdOrderBySortOrderAscIdAsc(String requestPriceHeaderId);
}
