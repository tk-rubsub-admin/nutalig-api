package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_request_step_role", uniqueConstraints = @UniqueConstraint(name = "uk_approval_step_role", columnNames = {"approval_request_step_id", "role_code"}))
public class ApprovalRequestStepRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_step_id", nullable = false)
    private ApprovalRequestStepEntity approvalRequestStep;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    public ApprovalRequestStepRoleEntity() { }

    public ApprovalRequestStepRoleEntity(String roleCode) { this.roleCode = roleCode; }
}
