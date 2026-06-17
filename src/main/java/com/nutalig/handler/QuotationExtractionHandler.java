package com.nutalig.handler;

import com.nutalig.service.AiExecutionService;
import com.nutalig.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.nutalig.utils.ObjectUtil.extractJsonObject;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationExtractionHandler implements PromptResultHandler {

    private final AiExecutionService aiExecutionService;
    private final QuotationService quotationService;

    @Override
    public String promptCode() {
        return "QUOTATION_EXTRACTION";
    }

    @Override
    public void handle(String userId, String message) throws Exception {
        log.info("Handle quotation extraction");

        String json = aiExecutionService.execute(this.promptCode(), Map.of("message", message));
        String cleanedJson = extractJsonObject(json);

        quotationService.createQuotationFromLine(userId, cleanedJson);
    }
}
