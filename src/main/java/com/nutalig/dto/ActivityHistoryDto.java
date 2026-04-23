package com.nutalig.dto;

import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ActivityHistoryDto {

    private Long id;
    private ActivityEntityType entityType;
    private String referenceId;
    private String actorId;
    private ActivityActorType actorType;
    private ActivityAction action;
    private ZonedDateTime actionAt;
    private ActivitySource source;
    private String summary;
    private String detailJson;
    private String ipAddress;
    private String requestId;
    private String traceId;
    private ZonedDateTime createdDate;
}
