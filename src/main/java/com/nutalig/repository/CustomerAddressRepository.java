package com.nutalig.repository;

import com.nutalig.constant.AddressType;
import com.nutalig.entity.CustomerAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, Long> {
    List<CustomerAddressEntity> findByCustomerId(String customerId);

    Optional<CustomerAddressEntity> findFirstByCustomerIdAndIsDefaultTrueAndAddressType(
            String customerId, AddressType addressType
    );
}