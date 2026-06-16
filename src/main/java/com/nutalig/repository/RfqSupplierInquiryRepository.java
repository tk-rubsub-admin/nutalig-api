package com.nutalig.repository;

import com.nutalig.entity.RfqSupplierInquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqSupplierInquiryRepository extends JpaRepository<RfqSupplierInquiryEntity, String> {

    List<RfqSupplierInquiryEntity> findAllByRequestPriceHeader_IdOrderByVersionNoDesc(String rfqId);

    Optional<RfqSupplierInquiryEntity> findFirstByRequestPriceHeader_IdOrderByVersionNoDesc(String rfqId);

    Optional<RfqSupplierInquiryEntity> findByIdAndRequestPriceHeader_Id(String inquiryId, String rfqId);

    @Query("""
            select coalesce(max(inquiry.versionNo), 0)
            from RfqSupplierInquiry inquiry
            where inquiry.requestPriceHeader.id = :rfqId
            """)
    Integer findMaxVersionNoByRfqId(String rfqId);
}
