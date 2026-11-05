package com.nutalig.repository;

import com.nutalig.entity.InvoicePaymentAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoicePaymentAttachmentRepository extends JpaRepository<InvoicePaymentAttachmentEntity, Long> {
    Optional<InvoicePaymentAttachmentEntity> findByIdAndInvoicePaymentId(Long id, Long paymentId);
}
