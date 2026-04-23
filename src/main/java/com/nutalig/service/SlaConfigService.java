package com.nutalig.service;

import com.nutalig.controller.sla.request.CreateSlaConfigRequest;
import com.nutalig.controller.sla.request.UpdateSlaConfigRequest;
import com.nutalig.dto.SlaConfigDto;
import com.nutalig.entity.SlaConfigEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SlaConfigMapper;
import com.nutalig.repository.SlaConfigRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaConfigService {

    private final UserProfileService userProfileService;
    private final SlaConfigRepository slaConfigRepository;
    private final SlaConfigMapper slaConfigMapper;

    @Transactional(readOnly = true)
    public List<SlaConfigDto> getAllSlaConfigs() {
        log.info("Get all sla configs");

        List<SlaConfigEntity> entities = slaConfigRepository.findAll(Sort.by(Sort.Order.asc("slaCode")));
        return slaConfigMapper.toDtoList(entities).stream()
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "slaConfigById", key = "#id")
    public SlaConfigDto getSlaConfigById(String id) throws DataNotFoundException {
        log.info("Get sla config by id {}", id);

        SlaConfigEntity entity = slaConfigRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("SLA config " + id + " not found."));

        return slaConfigMapper.toDto(entity);
    }

    @Transactional
    public SlaConfigDto createSlaConfig(CreateSlaConfigRequest request, String userId) throws InvalidRequestException {
        log.info("Create sla config request {}", request);

        validateCreateRequest(request);

        String slaCode = request.getSlaCode().trim();
        if (slaConfigRepository.findById(slaCode).isPresent()) {
            throw new InvalidRequestException("SLA config " + slaCode + " already exists.");
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        SlaConfigEntity entity = new SlaConfigEntity();
        entity.setSlaCode(slaCode);
        entity.setSlaName(StringUtils.trimToNull(request.getSlaName()));
        entity.setTargetDays(request.getTargetDays());
        entity.setDayType(request.getDayType());
        entity.setStatus(request.getStatus());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setCreatedDate(now);
        entity.setCreatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(now);
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));

        entity = slaConfigRepository.save(entity);
        return slaConfigMapper.toDto(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = "slaConfigById", key = "#id")
    public SlaConfigDto updateSlaConfig(String id, UpdateSlaConfigRequest request, String userId)
            throws DataNotFoundException {
        log.info("Update sla config {} request {}", id, request);

        SlaConfigEntity entity = slaConfigRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("SLA config " + id + " not found."));

        if (request.getSlaName() != null) {
            entity.setSlaName(StringUtils.trimToNull(request.getSlaName()));
        }
        if (request.getTargetDays() != null) {
            entity.setTargetDays(request.getTargetDays());
        }
        if (request.getDayType() != null) {
            entity.setDayType(request.getDayType());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getEffectiveFrom() != null) {
            entity.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveTo() != null) {
            entity.setEffectiveTo(request.getEffectiveTo());
        }
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));

        entity = slaConfigRepository.save(entity);
        return slaConfigMapper.toDto(entity);
    }

    public Integer calculateDayLeft(SlaConfigDto dto, LocalDate referenceDate) {
        if (dto == null || dto.getTargetDays() == null || referenceDate == null) {
            return null;
        }

        if (dto.getTargetDays() <= 0) {
            return 0;
        }

        LocalDate today = LocalDate.now(DateUtil.getTimeZone());
        LocalDate remainingFrom = today.isAfter(referenceDate) ? today : referenceDate;
        LocalDate dueDate = calculateDueDate(dto, referenceDate);

        if (remainingFrom.isAfter(dueDate)) {
            return 0;
        }

        if (dto.getDayType() == null || dto.getDayType() == com.nutalig.constant.SlaDayType.CALENDAR_DAY) {
            return Math.toIntExact(ChronoUnit.DAYS.between(remainingFrom, dueDate) + 1);
        }
        return countBusinessDays(remainingFrom, dueDate);
    }

    public ZonedDateTime calculateSlaDate(SlaConfigDto dto, ZonedDateTime referenceDateTime) {
        if (dto == null || referenceDateTime == null || dto.getTargetDays() == null || dto.getTargetDays() <= 0) {
            return null;
        }

        LocalDate dueDate = calculateDueDate(dto, referenceDateTime.toLocalDate());
        return ZonedDateTime.of(dueDate, referenceDateTime.toLocalTime(), referenceDateTime.getZone());
    }

    private LocalDate calculateDueDate(SlaConfigDto dto, LocalDate referenceDate) {
        if (dto.getDayType() == null || dto.getDayType() == com.nutalig.constant.SlaDayType.CALENDAR_DAY) {
            return referenceDate.plusDays(dto.getTargetDays() - 1L);
        }
        return addBusinessDays(referenceDate, dto.getTargetDays());
    }

    private LocalDate addBusinessDays(LocalDate startDate, int businessDays) {
        LocalDate currentDate = startDate;
        int countedDays = 0;

        while (true) {
            if (isBusinessDay(currentDate)) {
                countedDays++;
                if (countedDays == businessDays) {
                    return currentDate;
                }
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    private int countBusinessDays(LocalDate startDate, LocalDate endDate) {
        int businessDays = 0;
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            if (isBusinessDay(currentDate)) {
                businessDays++;
            }
        }
        return businessDays;
    }

    private boolean isBusinessDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private void validateCreateRequest(CreateSlaConfigRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }
        if (StringUtils.isBlank(request.getSlaCode())) {
            throw new InvalidRequestException("slaCode is required.");
        }
        if (StringUtils.isBlank(request.getSlaName())) {
            throw new InvalidRequestException("slaName is required.");
        }
        if (request.getTargetDays() == null) {
            throw new InvalidRequestException("targetDays is required.");
        }
        if (request.getDayType() == null) {
            throw new InvalidRequestException("dayType is required.");
        }
        if (request.getStatus() == null) {
            throw new InvalidRequestException("status is required.");
        }
    }
}
