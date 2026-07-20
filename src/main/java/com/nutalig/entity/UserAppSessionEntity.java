package com.nutalig.entity;

import com.nutalig.constant.AppSessionDeviceType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Entity(name = "UserAppSession")
@Table(
        name = "user_app_session",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_app_session_user_device", columnNames = {"user_id", "device_type"})
        }
)
public class UserAppSessionEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 30)
    private AppSessionDeviceType deviceType;

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;
}
