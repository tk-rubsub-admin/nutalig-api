package com.nutalig.repository;

import com.nutalig.entity.InvoicePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, Long> {
}
