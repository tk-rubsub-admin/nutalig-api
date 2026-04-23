package com.nutalig.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import com.nutalig.constant.RequestHeader;
import com.nutalig.dto.ActivityHistoryDto;
import com.nutalig.entity.ActivityHistoryEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.mapper.ActivityHistoryMapper;
import com.nutalig.repository.ActivityHistoryRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.utils.DateUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityHistoryService {

    private final ActivityHistoryRepository activityHistoryRepository;
    private final UserRepository userRepository;
    private final ActivityHistoryMapper activityHistoryMapper;
    private final ObjectMapper objectMapper;

    public ActivityHistoryEntity record(
            ActivityEntityType entityType,
            String referenceId,
            String actorId,
            ActivityActorType actorType,
            ActivityAction action,
            ActivitySource source,
            String summary,
            Object detail
    ) {
        return record(ActivityHistoryCommand.builder()
                .entityType(entityType)
                .referenceId(referenceId)
                .actorId(actorId)
                .actorType(actorType)
                .action(action)
                .source(source)
                .summary(summary)
                .detail(detail)
                .build());
    }

    public ActivityHistoryEntity record(ActivityHistoryCommand command) {
        String actor = command.getActorId();

        Optional<UserEntity> actorOptional = userRepository.findById(command.getActorId());
        if (!actorOptional.isEmpty()) {
            actor = actorOptional.get().getDisplayName();
        }
        ActivityHistoryEntity entity = new ActivityHistoryEntity();
        entity.setEntityType(command.getEntityType());
        entity.setReferenceId(command.getReferenceId());
        entity.setActorId(actor);
        entity.setActorType(command.getActorType());
        entity.setAction(command.getAction());
        entity.setSource(command.getSource());
        entity.setSummary(command.getSummary());
        entity.setDetailJson(toJson(command.getDetail()));
        entity.setIpAddress(StringUtils.defaultIfBlank(command.getIpAddress(), resolveIpAddress()));
        entity.setRequestId(StringUtils.defaultIfBlank(command.getRequestId(), resolveRequestId()));
        entity.setTraceId(StringUtils.defaultIfBlank(command.getTraceId(), resolveTraceId()));
        entity.setActionAt(DateUtil.getTimeZone() == null ? java.time.ZonedDateTime.now() : java.time.ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setCreatedDate(DateUtil.getTimeZone() == null ? java.time.ZonedDateTime.now() : java.time.ZonedDateTime.now(DateUtil.getTimeZone()));

        ActivityHistoryEntity savedEntity = activityHistoryRepository.save(entity);
        log.info("Saved activity history entityType={}, referenceId={}, action={}",
                savedEntity.getEntityType(), savedEntity.getReferenceId(), savedEntity.getAction());
        return savedEntity;
    }

    public List<ActivityHistoryDto> getHistory(ActivityEntityType entityType, String referenceId) {
        return activityHistoryMapper.toDtoList(
                activityHistoryRepository.findByEntityTypeAndReferenceIdOrderByActionAtDesc(entityType, referenceId)
        );
    }

    public String resolveCurrentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        return StringUtils.trimToNull(request.getHeader(RequestHeader.USER_ID));
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }

        if (detail instanceof String detailString) {
            return detailString;
        }

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialize activity history detail", e);
            return "{\"error\":\"serialize_detail_failed\"}";
        }
    }

    private String resolveRequestId() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String headerValue = request.getHeader(RequestHeader.REQUEST_ID);
            if (StringUtils.isNotBlank(headerValue)) {
                return headerValue;
            }
        }

        return StringUtils.defaultIfBlank(MDC.get("requestId"), MDC.get("X-Request-Id"));
    }

    private String resolveTraceId() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String headerValue = request.getHeader(RequestHeader.TRACE_ID);
            if (StringUtils.isNotBlank(headerValue)) {
                return headerValue;
            }
        }

        return StringUtils.defaultIfBlank(MDC.get("traceId"), MDC.get("X-Trace-Id"));
    }

    private String resolveIpAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    @Value
    @Builder
    public static class ActivityHistoryCommand {
        ActivityEntityType entityType;
        String referenceId;
        String actorId;
        ActivityActorType actorType;
        ActivityAction action;
        ActivitySource source;
        String summary;
        Object detail;
        String ipAddress;
        String requestId;
        String traceId;
    }
}
