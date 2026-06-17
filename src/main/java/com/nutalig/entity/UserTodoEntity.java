package com.nutalig.entity;

import com.nutalig.constant.UserTodoPriority;
import com.nutalig.constant.UserTodoStatus;
import com.nutalig.constant.UserTodoType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_todo")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTodoEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", referencedColumnName = "id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private UserEntity ownerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "todo_type", nullable = false, length = 50)
    private UserTodoType todoType = UserTodoType.GENERAL;

    @Column(name = "title", nullable = false)
    @ToString.Include
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserTodoStatus status = UserTodoStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private UserTodoPriority priority = UserTodoPriority.MEDIUM;

    @Column(name = "target_module", length = 64)
    private String targetModule;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "target_path", length = 512)
    private String targetPath;

    @Column(name = "due_date")
    private ZonedDateTime dueDate;

    @Column(name = "completed_date")
    private ZonedDateTime completedDate;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
