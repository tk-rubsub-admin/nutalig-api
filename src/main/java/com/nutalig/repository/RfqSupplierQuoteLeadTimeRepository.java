package com.nutalig.repository;

import com.nutalig.entity.RfqSupplierQuoteLeadTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RfqSupplierQuoteLeadTimeRepository extends JpaRepository<RfqSupplierQuoteLeadTimeEntity, Long> {
}
