package com.nutalig.repository;

import com.nutalig.entity.SupplierShippingRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierShippingRouteRepository extends JpaRepository<SupplierShippingRouteEntity, Long> {
    List<SupplierShippingRouteEntity> findAllByActiveTrueOrderByIdAsc();
    List<SupplierShippingRouteEntity> findAllBySupplierShippingIdAndActiveTrueOrderByIdAsc(Long supplierShippingId);
    Optional<SupplierShippingRouteEntity> findByIdAndActiveTrue(Long id);
}
