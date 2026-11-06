package com.nutalig.repository;

import com.nutalig.entity.CustomerBillingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerBillingProfileRepository extends JpaRepository<CustomerBillingProfileEntity, Long> {
    List<CustomerBillingProfileEntity> findByCustomerIdAndActiveTrueOrderByIsDefaultDescIdAsc(String customerId);

    Optional<CustomerBillingProfileEntity> findFirstByCustomerIdAndIsDefaultTrueAndActiveTrue(String customerId);
}
