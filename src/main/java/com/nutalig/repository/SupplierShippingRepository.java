package com.nutalig.repository;

import com.nutalig.entity.SupplierShippingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierShippingRepository extends JpaRepository<SupplierShippingEntity, Long> {
    List<SupplierShippingEntity> findAllByActiveTrueOrderByShippingMethodAscIdAsc();

    Optional<SupplierShippingEntity> findByIdAndActiveTrue(Long id);
}
