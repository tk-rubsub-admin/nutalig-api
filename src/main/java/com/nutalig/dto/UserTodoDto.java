package com.nutalig.dto;

import com.nutalig.constant.UserTodoPriority;
import com.nutalig.constant.UserTodoStatus;
import com.nutalig.constant.UserTodoType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class UserTodoDto {
    private Long id;
    private String ownerUserId;
    private UserTodoType todoType;
    private String title;
    private String description;
    private UserTodoStatus status;
    private UserTodoPriority priority;
    private String targetModule;
    private String targetId;
    private String targetPath;
    private ZonedDateTime dueDate;
    private ZonedDateTime completedDate;
    private Integer sortOrder;
    private Boolean active;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
