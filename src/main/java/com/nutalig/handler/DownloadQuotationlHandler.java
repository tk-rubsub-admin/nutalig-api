package com.nutalig.handler;

import com.nutalig.service.QuotationService;
import com.nutalig.service.RFQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadQuotationlHandler implements PromptResultHandler {

    private static final String DOWNLOAD_QUOTATION_PREFIX = "ใบเสนอราคา เลขที่:";
    private final QuotationService quotationService;
    @Override
    public String promptCode() {
        return "DOWNLOAD_QUOTATION";
    }

    @Override
    public void handle(String userId, String message) throws Exception {
        log.info("Handle download quotaion");

        String id = message.replaceAll(DOWNLOAD_QUOTATION_PREFIX, "");

        quotationService.getAndDownloadQuotation(userId, id);
    }
}
