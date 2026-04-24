package com.nutalig.entity;

import com.nutalig.constant.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.ZonedDateTime;

@Data
@Table(name = "user")
@Entity(name = "User")
public class UserEntity extends AuditDateEntity {

    @Id
    @GenericGenerator(name = "userIdGenerator",
            parameters = {
                @org.hibernate.annotations.Parameter(name = "prefix", value = "USER"),
                @org.hibernate.annotations.Parameter(name = "length", value = "%06d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @GeneratedValue(generator = "userIdGenerator")
    private String id;

    @Column(name = "username")
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "role_code", referencedColumnName = "role_code")
    private UserRoleEntity userRoleEntity;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "line_user_id")
    private String lineUserId;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "verified_date")
    private ZonedDateTime verifiedDate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private EmployeeEntity employeeEntity;

}
