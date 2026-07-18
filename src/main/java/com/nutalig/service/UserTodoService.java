package com.nutalig.service;

import com.nutalig.constant.UserTodoPriority;
import com.nutalig.constant.UserTodoStatus;
import com.nutalig.constant.UserTodoType;
import com.nutalig.controller.user.request.CreateUserTodoRequest;
import com.nutalig.dto.UserTodoDto;
import com.nutalig.entity.UserEntity;
import com.nutalig.entity.UserTodoEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRepository;
import com.nutalig.repository.UserTodoRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTodoService {

    private final UserTodoRepository userTodoRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserTodoDto> getTodosByUser(String userId, List<UserTodoStatus> statuses, UserTodoType todoType) {
        List<UserTodoEntity> todos;
        List<UserTodoStatus> normalizedStatuses = statuses == null
                ? List.of()
                : statuses.stream().filter(Objects::nonNull).distinct().toList();

        if (!normalizedStatuses.isEmpty() && todoType != null) {
            todos = userTodoRepository
                    .findAllByOwnerUser_IdAndTodoTypeAndStatusInAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
                            userId,
                            todoType,
                            normalizedStatuses
                    );
        } else if (!normalizedStatuses.isEmpty()) {
            todos = userTodoRepository
                    .findAllByOwnerUser_IdAndStatusInAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
                            userId,
                            normalizedStatuses
                    );
        } else if (todoType != null) {
            todos = userTodoRepository
                    .findAllByOwnerUser_IdAndTodoTypeAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(
                            userId,
                            todoType
                    );
        } else {
            todos = userTodoRepository
                    .findAllByOwnerUser_IdAndActiveTrueOrderBySortOrderAscDueDateAscCreatedDateDesc(userId);
        }

        return todos.stream().map(this::toDto).toList();
    }

    @Transactional
    public UserTodoDto markTodoAsDone(String userId, Long todoId) throws DataNotFoundException {
        UserTodoEntity entity = userTodoRepository.findByIdAndOwnerUser_IdAndActiveTrue(todoId, userId)
                .orElseThrow(() -> new DataNotFoundException("Todo " + todoId + " not found."));

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        entity.setStatus(UserTodoStatus.DONE);
        entity.setCompletedDate(now);
        entity.setUpdatedBy(userId);

        return toDto(userTodoRepository.save(entity));
    }

    @Transactional
    public UserTodoDto createTodoForUser(String userId, CreateUserTodoRequest request)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null || StringUtils.isBlank(request.getTitle())) {
            throw new InvalidRequestException("Title is required.");
        }

        UserEntity ownerUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        UserTodoEntity entity = buildUserTodoEntity(
                ownerUser,
                request.getTodoType() != null ? request.getTodoType() : UserTodoType.GENERAL,
                request.getTitle(),
                request.getDescription(),
                UserTodoStatus.TODO,
                request.getPriority() != null ? request.getPriority() : UserTodoPriority.MEDIUM,
                request.getTargetModule(),
                request.getTargetId(),
                request.getTargetPath(),
                request.getDueDate(),
                request.getSortOrder(),
                userId
        );

        return toDto(entity);
    }

    public UserTodoEntity buildUserTodoEntity(
            UserEntity ownerUser,
            UserTodoType todoType,
            String title,
            String description,
            UserTodoStatus status,
            UserTodoPriority priority,
            String targetModule,
            String targetId,
            String targetPath,
            ZonedDateTime dueDate,
            Integer sortOrder,
            String userId
    ) {
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        log.info("Create to-do {} for user {}", title, ownerUser.getDisplayName());

        UserTodoEntity entity = new UserTodoEntity();
        entity.setOwnerUser(ownerUser);
        entity.setTodoType(todoType != null ? todoType : UserTodoType.GENERAL);
        entity.setTitle(StringUtils.trimToNull(title));
        entity.setDescription(StringUtils.trimToNull(description));
        entity.setStatus(status != null ? status : UserTodoStatus.TODO);
        entity.setPriority(priority != null ? priority : com.nutalig.constant.UserTodoPriority.MEDIUM);
        entity.setTargetModule(StringUtils.trimToNull(targetModule));
        entity.setTargetId(StringUtils.trimToNull(targetId));
        entity.setTargetPath(StringUtils.trimToNull(targetPath));
        entity.setDueDate(dueDate);
        entity.setSortOrder(sortOrder);
        entity.setActive(Boolean.TRUE);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setCreatedDate(now);
        entity.setUpdatedDate(now);

        if (entity.getStatus() == UserTodoStatus.DONE) {
            entity.setCompletedDate(now);
        }

        userTodoRepository.save(entity);

        return entity;
    }

    @Transactional(readOnly = true)
    public List<UserTodoEntity> findActiveTodosByTarget(String targetModule, String targetId, List<UserTodoStatus> statuses) {
        List<UserTodoStatus> normalizedStatuses = statuses == null
                ? List.of()
                : statuses.stream().filter(Objects::nonNull).distinct().toList();

        if (!normalizedStatuses.isEmpty()) {
            return userTodoRepository.findAllByTargetModuleAndTargetIdAndStatusInAndActiveTrueOrderByCreatedDateDesc(
                    StringUtils.trimToNull(targetModule),
                    StringUtils.trimToNull(targetId),
                    normalizedStatuses
            );
        }

        return userTodoRepository.findAllByTargetModuleAndTargetIdAndActiveTrueOrderByCreatedDateDesc(
                StringUtils.trimToNull(targetModule),
                StringUtils.trimToNull(targetId)
        );
    }

    @Transactional
    public void markTodosAsDone(List<UserTodoEntity> todos, String userId) {
        if (todos == null || todos.isEmpty()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        for (UserTodoEntity todo : todos) {
            todo.setStatus(UserTodoStatus.DONE);
            todo.setCompletedDate(now);
            todo.setUpdatedBy(userId);
            todo.setUpdatedDate(now);
        }

        userTodoRepository.saveAll(todos);
    }

    private UserTodoDto toDto(UserTodoEntity entity) {
        UserTodoDto dto = new UserTodoDto();
        dto.setId(entity.getId());
        dto.setOwnerUserId(entity.getOwnerUser() != null ? entity.getOwnerUser().getId() : null);
        dto.setTodoType(entity.getTodoType());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setPriority(entity.getPriority());
        dto.setTargetModule(entity.getTargetModule());
        dto.setTargetId(entity.getTargetId());
        dto.setTargetPath(entity.getTargetPath());
        dto.setDueDate(entity.getDueDate());
        dto.setCompletedDate(entity.getCompletedDate());
        dto.setSortOrder(entity.getSortOrder());
        dto.setActive(entity.getActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }
}
