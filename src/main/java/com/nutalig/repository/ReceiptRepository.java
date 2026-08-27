package com.nutalig.repository;

import com.nutalig.entity.ReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptEntity, String>, JpaSpecificationExecutor<ReceiptEntity> {
    @Query(value = """
            select count(*)
            from receipt
            where customer_address_id = :customerAddressId
            """, nativeQuery = true)
    long countByCustomerAddress_Id(@Param("customerAddressId") Long customerAddressId);
}
