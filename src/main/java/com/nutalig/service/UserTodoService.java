package com.nutalig.service;

import com.nutalig.constant.UserTodoStatus;
import com.nutalig.constant.UserTodoType;
import com.nutalig.dto.UserTodoDto;
import com.nutalig.entity.UserTodoEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.repository.UserTodoRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserTodoService {

    private final UserTodoRepository userTodoRepository;

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
