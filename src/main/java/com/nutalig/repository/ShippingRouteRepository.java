package com.nutalig.repository;

import com.nutalig.entity.ShippingRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingRouteRepository extends JpaRepository<ShippingRouteEntity, Long> {
    List<ShippingRouteEntity> findAllByActiveTrueOrderByIdAsc();
    Optional<ShippingRouteEntity> findByIdAndActiveTrue(Long id);
    boolean existsByOriginLocation_IdAndDestinationLocation_IdAndActiveTrue(Long originLocationId,
                                                                              Long destinationLocationId);
}
