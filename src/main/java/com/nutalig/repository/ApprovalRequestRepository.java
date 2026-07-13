package com.nutalig.repository;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ApprovalRequestStatus;
import com.nutalig.entity.ApprovalRequestEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long> {

    @EntityGraph(attributePaths = {"steps", "steps.approverUser", "steps.actedByUser"})
    Optional<ApprovalRequestEntity> findById(Long id);

    @EntityGraph(attributePaths = {"steps", "steps.approverUser", "steps.actedByUser"})
    Optional<ApprovalRequestEntity> findFirstByEntityTypeAndReferenceIdAndStatusInOrderByCreatedDateDesc(
            ActivityEntityType entityType,
            String referenceId,
            List<ApprovalRequestStatus> statuses
    );
}
