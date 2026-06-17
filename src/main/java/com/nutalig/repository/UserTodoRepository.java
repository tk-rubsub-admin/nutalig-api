package com.nutalig.repository;

import com.nutalig.constant.UserTodoStatus;
import com.nutalig.constant.UserTodoType;
import com.nutalig.entity.UserTodoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTodoRepository extends JpaRepository<UserTodoEntity, Long> {

    List<UserTodoEntity> findAllByOwnerUser_IdAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
            String ownerUserId
    );

    Optional<UserTodoEntity> findByIdAndOwnerUser_IdAndActiveTrue(Long id, String ownerUserId);

    List<UserTodoEntity> findAllByOwnerUser_IdAndStatusInAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
            String ownerUserId,
            List<UserTodoStatus> statuses
    );

    List<UserTodoEntity> findAllByOwnerUser_IdAndTodoTypeAndStatusInAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
            String ownerUserId,
            UserTodoType todoType,
            List<UserTodoStatus> statuses
    );

    List<UserTodoEntity> findAllByOwnerUser_IdAndTodoTypeAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
            String ownerUserId,
            UserTodoType todoType
    );

    List<UserTodoEntity> findAllByOwnerUser_IdAndActiveTrueAndDueDateBetweenOrderByDueDateAsc(
            String ownerUserId,
            ZonedDateTime startDate,
            ZonedDateTime endDate
    );

    List<UserTodoEntity> findAllByTargetModuleAndTargetIdAndActiveTrueOrderByCreatedDateDesc(
            String targetModule,
            String targetId
    );
}
