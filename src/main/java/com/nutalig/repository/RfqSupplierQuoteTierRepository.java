package com.nutalig.repository;

import com.nutalig.entity.RfqSupplierQuoteTierEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RfqSupplierQuoteTierRepository extends JpaRepository<RfqSupplierQuoteTierEntity, Long> {
    @EntityGraph(attributePaths = {"quoteDetail", "quoteDetail.supplierQuote", "quoteDetail.supplierQuote.packages"})
    List<RfqSupplierQuoteTierEntity> findAllByIdIn(Collection<Long> ids);
}
