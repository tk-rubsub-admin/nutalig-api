package com.nutalig.handler;

import com.nutalig.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuotationExtractionHandler implements PromptResultHandler {

    private final QuotationService quotationService;

    @Override
    public String promptCode() {
        return "QUOTATION_EXTRACTION";
    }

    @Override
    public void handle(String userId, String cleanedJson) throws Exception {
        quotationService.createQuotationFromLine(userId, cleanedJson);
    }
}
