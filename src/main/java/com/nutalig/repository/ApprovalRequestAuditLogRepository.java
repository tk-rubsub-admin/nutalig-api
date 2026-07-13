package com.nutalig.repository;

import com.nutalig.entity.ApprovalRequestAuditLogEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestAuditLogRepository extends JpaRepository<ApprovalRequestAuditLogEntity, Long> {

    @EntityGraph(attributePaths = {"actorUser"})
    List<ApprovalRequestAuditLogEntity> findAllByApprovalRequest_IdOrderByCreatedDateAscIdAsc(Long approvalRequestId);
}
