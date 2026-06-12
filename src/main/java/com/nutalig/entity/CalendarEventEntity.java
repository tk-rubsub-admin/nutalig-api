package com.nutalig.entity;

import com.nutalig.constant.CalendarEventType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "calendar_event")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CalendarEventEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(name = "title", nullable = false)
    @ToString.Include
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    @ToString.Include
    private CalendarEventType eventType;

    @Column(name = "start_at", nullable = false)
    @ToString.Include
    private ZonedDateTime startAt;

    @Column(name = "end_at", nullable = false)
    @ToString.Include
    private ZonedDateTime endAt;

    @Column(name = "all_day", nullable = false)
    private Boolean allDay = Boolean.FALSE;

    @Column(name = "color_code", length = 32)
    private String colorCode;

    @Column(name = "remark", columnDefinition = "text")
    private String remark;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
