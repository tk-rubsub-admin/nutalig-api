package com.nutalig.handler;

import com.nutalig.service.AiExecutionService;
import com.nutalig.service.RFQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.nutalig.utils.ObjectUtil.extractJsonObject;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinalRfqHandler implements PromptResultHandler {

    private final AiExecutionService aiExecutionService;
    private final RFQService rfqService;
    @Override
    public String promptCode() {
        return "FINAL_RFQ";
    }

    @Override
    public void handle(String userId, String message) throws Exception {
        log.info("Handle final RFQ");

        String json = aiExecutionService.execute(this.promptCode(), Map.of("message", message));
        String cleanedJson = extractJsonObject(json);

        rfqService.finalRfqFromLine(userId, cleanedJson);
    }
}
