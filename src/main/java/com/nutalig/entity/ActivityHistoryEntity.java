package com.nutalig.entity;

import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "activity_history")
public class ActivityHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 30, nullable = false)
    private ActivityEntityType entityType;

    @Column(name = "reference_id", length = 100, nullable = false)
    private String referenceId;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 30, nullable = false)
    private ActivityActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50, nullable = false)
    private ActivityAction action;

    @Column(name = "action_at", nullable = false)
    private ZonedDateTime actionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30)
    private ActivitySource source;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "detail_json", columnDefinition = "json")
    private String detailJson;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "created_date", nullable = false)
    private ZonedDateTime createdDate;
}
