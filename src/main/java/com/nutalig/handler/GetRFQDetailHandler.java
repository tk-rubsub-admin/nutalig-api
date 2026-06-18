package com.nutalig.handler;

import com.nutalig.service.RFQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetRFQDetailHandler implements PromptResultHandler {

    private static final String RFQ_DETAIL_PREFIX = "คำขอราคา เลขที่:";
    private final RFQService rfqService;
    @Override
    public String promptCode() {
        return "RFQ_DETAIL";
    }

    @Override
    public void handle(String userId, String message) throws Exception {
        log.info("Handle get RFQ detail");

        String id = message.replaceAll(RFQ_DETAIL_PREFIX, "");

        rfqService.getRFQAndSendMessageToLine(userId, id);
    }
}
