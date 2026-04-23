package com.nutalig.repository;

import com.nutalig.entity.RequestPriceTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestPriceTierRepository extends JpaRepository<RequestPriceTierEntity, Long> {

    List<RequestPriceTierEntity> findByRequestPriceDetailIdOrderBySortOrderAscIdAsc(Long requestPriceDetailId);
}
