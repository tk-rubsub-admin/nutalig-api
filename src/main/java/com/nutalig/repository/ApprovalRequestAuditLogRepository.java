package com.nutalig.repository;

import com.nutalig.entity.ApprovalRequestAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRequestAuditLogRepository extends JpaRepository<ApprovalRequestAuditLogEntity, Long> {
}
