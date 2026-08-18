package com.nutalig.schedule;

import com.nutalig.config.AppProperties;
import com.nutalig.dto.RfqHeaderDto;
import com.nutalig.service.RFQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RfqPendingSupplierQuotedScheduler {

    private final AppProperties appProperties;
    private final RFQService rfqService;

    @Scheduled(cron = "${app.rfq-pending-supplier-quoted.cron:0 1 10 * * *}", zone = "Asia/Bangkok")
    public void collectPendingSupplierQuotedRfqs() {
        runPendingSupplierQuotedCollection();
    }

    public List<RfqHeaderDto> runPendingSupplierQuotedCollection() {
        AppProperties.RfqPendingSupplierQuoted config = appProperties.getRfqPendingSupplierQuoted();
        if (config == null || !config.isEnabled()) {
            log.debug("Skip pending supplier quoted RFQ scheduler because it is disabled.");
            return List.of();
        }

        List<RfqHeaderDto> rfqs = rfqService.getPendingSupplierQuotedRfqs();
        rfqService.sendPendingSupplierQuotedSummaryNotifications(rfqs);
        log.info(
                "Pending supplier quoted RFQ scheduler completed. count={}, rfqIds={}",
                rfqs.size(),
                rfqs.stream().map(RfqHeaderDto::getId).toList()
        );
        return rfqs;
    }
}
