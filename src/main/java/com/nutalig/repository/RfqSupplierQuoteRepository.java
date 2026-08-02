package com.nutalig.repository;

import com.nutalig.entity.RfqSupplierQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqSupplierQuoteRepository extends JpaRepository<RfqSupplierQuoteEntity, String> {

    List<RfqSupplierQuoteEntity> findAllByRequestPriceHeader_IdOrderByUpdatedDateDesc(String rfqId);

    Optional<RfqSupplierQuoteEntity> findByIdAndRequestPriceHeader_Id(String quoteId, String rfqId);

    Optional<RfqSupplierQuoteEntity> findFirstByRequestPriceHeader_IdAndSupplier_IdOrderByRevisionNoDesc(
            String rfqId,
            String supplierId
    );

    long countByRequestPriceHeader_Id(String rfqId);

    @Query("""
            select coalesce(max(q.revisionNo), 0)
            from RfqSupplierQuote q
            where q.requestPriceHeader.id = :rfqId
              and q.supplier.id = :supplierId
            """)
    Integer findMaxRevisionNoByRequestPriceHeader_IdAndSupplier_Id(String rfqId, String supplierId);
}
