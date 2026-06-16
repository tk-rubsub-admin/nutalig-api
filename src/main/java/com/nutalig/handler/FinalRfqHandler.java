package com.nutalig.handler;

import com.nutalig.service.RFQService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinalRfqHandler implements PromptResultHandler {

    private final RFQService rfqService;
    @Override
    public String promptCode() {
        return "FINAL_RFQ";
    }

    @Override
    public void handle(String userId, String cleanedJson) throws Exception {
        rfqService.finalRfqFromLine(userId, cleanedJson);
    }
}
