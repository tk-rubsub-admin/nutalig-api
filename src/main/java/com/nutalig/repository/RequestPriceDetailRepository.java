package com.nutalig.repository;

import com.nutalig.entity.RequestPriceDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceDetailRepository extends JpaRepository<RequestPriceDetailEntity, Long> {

    List<RequestPriceDetailEntity> findByRequestPriceHeaderIdOrderBySortOrderAscIdAsc(String requestPriceHeaderId);
}
