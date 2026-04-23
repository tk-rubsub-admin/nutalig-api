package com.nutalig.repository;

import com.nutalig.entity.QuotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationEntity, String>, JpaSpecificationExecutor<QuotationEntity> {
}
