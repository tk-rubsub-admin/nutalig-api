package com.nutalig.repository;

import com.nutalig.entity.QuotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationEntity, String>, JpaSpecificationExecutor<QuotationEntity> {
    List<QuotationEntity> findAllByRfqIdOrderByCreatedDateDesc(String rfqId);

    List<QuotationEntity> findAllByRfqIdIn(Collection<String> rfqIds);

    @Query(value = """
            select count(*)
            from quotations
            where customer_address_id = :customerAddressId
            """, nativeQuery = true)
    long countByCustomerAddress_Id(@Param("customerAddressId") Long customerAddressId);

    boolean existsByRfqId(String rfqId);
}
