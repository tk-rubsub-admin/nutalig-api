package com.nutalig.repository;

import com.nutalig.entity.SupplierAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierAttachmentRepository extends JpaRepository<SupplierAttachmentEntity, Long> {
    Optional<SupplierAttachmentEntity> findByIdAndSupplier_IdAndActiveTrue(Long id, String supplierId);
}
