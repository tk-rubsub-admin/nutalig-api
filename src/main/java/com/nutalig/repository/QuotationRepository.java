package com.nutalig.repository;

import com.nutalig.entity.QuotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationEntity, String>, JpaSpecificationExecutor<QuotationEntity> {
    List<QuotationEntity> findAllByRfqIdOrderByCreatedDateDesc(String rfqId);

    List<QuotationEntity> findAllByRfqIdIn(Collection<String> rfqIds);

    boolean existsByRfqId(String rfqId);
}
