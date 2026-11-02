package com.nutalig.repository;

import com.nutalig.entity.ShippingLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingLocationRepository extends JpaRepository<ShippingLocationEntity, Long> {
    List<ShippingLocationEntity> findAllByActiveTrueOrderByNameAscIdAsc();
    List<ShippingLocationEntity> findAllByActiveTrueOrderByIdAsc();
    Optional<ShippingLocationEntity> findByIdAndActiveTrue(Long id);
}
