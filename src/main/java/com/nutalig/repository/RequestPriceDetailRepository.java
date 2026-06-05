package com.nutalig.repository;

import com.nutalig.entity.RfqDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceDetailRepository extends JpaRepository<RfqDetailEntity, Long> {

    List<RfqDetailEntity> findByRequestPriceHeaderIdOrderBySortOrderAscIdAsc(String requestPriceHeaderId);
}
