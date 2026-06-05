package com.nutalig.repository;

import com.nutalig.entity.RfqSupplierQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqSupplierQuoteRepository extends JpaRepository<RfqSupplierQuoteEntity, String> {

    List<RfqSupplierQuoteEntity> findAllByRequestPriceHeader_IdOrderByUpdatedDateDesc(String rfqId);

    Optional<RfqSupplierQuoteEntity> findByIdAndRequestPriceHeader_Id(String quoteId, String rfqId);

    Optional<RfqSupplierQuoteEntity> findByRequestPriceHeader_IdAndSupplier_Id(String rfqId, String supplierId);
}
