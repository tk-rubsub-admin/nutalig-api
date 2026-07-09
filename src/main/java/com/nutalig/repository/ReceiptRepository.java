package com.nutalig.repository;

import com.nutalig.entity.ReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptEntity, String>, JpaSpecificationExecutor<ReceiptEntity> {
}
