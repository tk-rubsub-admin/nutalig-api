package com.nutalig.schedule;

import com.nutalig.config.AppProperties;
import com.nutalig.dto.RfqHeaderDto;
import com.nutalig.service.RFQService;
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

        List<RfqHeaderDto> rfqs = rfqService.getPendingAcceptanceRfqs();
        rfqService.sendPendingAcceptanceSummaryNotifications(rfqs);
        log.info(
                "Pending acceptance RFQ scheduler completed. count={}, rfqIds={}",
                rfqs.size(),
                rfqs.stream().map(RfqHeaderDto::getId).toList()
        );
        return rfqs;
    }
}
