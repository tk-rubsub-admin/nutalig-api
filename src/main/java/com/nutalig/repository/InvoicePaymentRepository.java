package com.nutalig.repository;

import com.nutalig.entity.InvoicePaymentEntity;
import com.nutalig.constant.InvoicePaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, Long> {
    List<InvoicePaymentEntity> findByStatusOrderByCreatedDateAsc(InvoicePaymentStatus status);
}
