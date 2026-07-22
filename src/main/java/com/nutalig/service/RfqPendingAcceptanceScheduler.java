package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.dto.RfqHeaderDto;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RfqPendingAcceptanceScheduler {

    private final AppProperties appProperties;
    private final RFQService rfqService;

    @Scheduled(cron = "${app.rfq-pending-acceptance.cron:0 0 10 * * *}", zone = "Asia/Bangkok")
    public void collectPendingAcceptanceRfqs() {
        runPendingAcceptanceCollection();
    }

    public List<RfqHeaderDto> runPendingAcceptanceCollection() {
        AppProperties.RfqPendingAcceptance config = appProperties.getRfqPendingAcceptance();
        if (config == null || !config.isEnabled()) {
            log.debug("Skip pending acceptance RFQ scheduler because it is disabled.");
            return List.of();
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        ZonedDateTime requestedDateStart = now.toLocalDate()
                .minusDays(config.getStartOffsetDays())
                .atTime(config.getStartTime())
                .atZone(DateUtil.getTimeZone());
        ZonedDateTime requestedDateEnd = now.toLocalDate()
                .minusDays(config.getEndOffsetDays())
                .atTime(config.getEndTime())
                .atZone(DateUtil.getTimeZone());

        if (requestedDateEnd.isBefore(requestedDateStart)) {
            throw new IllegalArgumentException("Configured RFQ pending acceptance window is invalid.");
        }

        List<RfqHeaderDto> rfqs = rfqService.getPendingAcceptanceRfqsInWindow(requestedDateStart, requestedDateEnd);
        rfqService.sendPendingAcceptanceSummaryNotifications(rfqs, requestedDateStart, requestedDateEnd);
        log.info(
                "Pending acceptance RFQ scheduler completed. windowStart={}, windowEnd={}, count={}, rfqIds={}",
                requestedDateStart,
                requestedDateEnd,
                rfqs.size(),
                rfqs.stream().map(RfqHeaderDto::getId).toList()
        );
        return rfqs;
    }
}
