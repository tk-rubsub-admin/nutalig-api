package com.nutalig.repository;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ApprovalRequestStatus;
import com.nutalig.constant.ApprovalRequestType;
import com.nutalig.entity.ApprovalRequestEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long> {

    @EntityGraph(attributePaths = {"steps", "steps.approverUser", "steps.actedByUser", "steps.approverRoles"})
    Optional<ApprovalRequestEntity> findById(Long id);

    Optional<ApprovalRequestEntity> findFirstByEntityTypeAndReferenceIdAndStatusInOrderByCreatedDateDesc(
            ActivityEntityType entityType,
            String referenceId,
            List<ApprovalRequestStatus> statuses
    );

    Optional<ApprovalRequestEntity> findFirstByEntityTypeAndReferenceIdAndRequestTypeOrderByCreatedDateDesc(
            ActivityEntityType entityType,
            String referenceId,
            ApprovalRequestType requestType
    );

    List<ApprovalRequestEntity> findAllByEntityTypeAndRequestType(
            ActivityEntityType entityType,
            ApprovalRequestType requestType
    );

    List<ApprovalRequestEntity> findAllByEntityTypeAndReferenceIdInAndRequestTypeOrderByCreatedDateDesc(
            ActivityEntityType entityType,
            List<String> referenceIds,
            ApprovalRequestType requestType
    );

    List<ApprovalRequestEntity> findAllByEntityTypeAndRequestTypeAndStatusOrderByRequestedDateDesc(
            ActivityEntityType entityType,
            ApprovalRequestType requestType,
            ApprovalRequestStatus status
    );

    boolean existsByEntityTypeAndReferenceIdAndRequestTypeAndStatus(
            ActivityEntityType entityType, String referenceId, ApprovalRequestType requestType, ApprovalRequestStatus status
    );
}
