package com.nutalig.repository;

import com.nutalig.entity.ApprovalRequestStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRequestStepRepository extends JpaRepository<ApprovalRequestStepEntity, Long> {
    Optional<ApprovalRequestStepEntity> findByApproveActionKey(String approveActionKey);
    Optional<ApprovalRequestStepEntity> findByRejectActionKey(String rejectActionKey);
}
