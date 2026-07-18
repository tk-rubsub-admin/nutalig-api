package com.nutalig.repository;

import com.nutalig.entity.SalesOrderAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderAttachmentRepository extends JpaRepository<SalesOrderAttachmentEntity, Long> {
    List<SalesOrderAttachmentEntity> findBySalesOrderSalesOrderNoAndActiveTrueOrderBySortOrderAscIdAsc(String salesOrderNo);
    Optional<SalesOrderAttachmentEntity> findByIdAndSalesOrderSalesOrderNoAndActiveTrue(Long id, String salesOrderNo);
}
