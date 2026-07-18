package com.nutalig.controller.user.request;

import com.nutalig.constant.UserTodoPriority;
import com.nutalig.constant.UserTodoType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CreateUserTodoRequest {
    private UserTodoType todoType;
    private String title;
    private String description;
    private UserTodoPriority priority;
    private String targetModule;
    private String targetId;
    private String targetPath;
    private ZonedDateTime dueDate;
    private Integer sortOrder;
}
