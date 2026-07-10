package com.nutalig.controller.line;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.controller.line.response.LineMessageWebhookResponse;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.UserDto;
import com.nutalig.dto.line.Event;
import com.nutalig.dto.line.ApprovalMessageData;
import com.nutalig.dto.line.Message;
import com.nutalig.service.LineHandleMessageService;
import com.nutalig.service.LineMessageService;
import com.nutalig.service.ApprovalService;
import com.nutalig.service.UserProfileService;
import com.nutalig.utils.LineSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LineController {

    private final LineConfiguration lineConfiguration;
    private final LineHandleMessageService lineHandleMessageService;
    private final LineMessageService lineMessageService;
    private final ApprovalService approvalService;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    @PostMapping("/line-webhook")
    public ResponseEntity<String> handleLineWebhook(@RequestBody String requestBody, @RequestHeader("X-Line-Signature") String signature) {
        log.info("Handle Line webhook");
        boolean isValid = LineSignatureValidator.validate(
                lineConfiguration.getLineMessageChannelSecret(),
                requestBody,
                signature
        );

        if (!isValid) {
            log.warn("Invalid LINE signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            LineMessageWebhookResponse webhookResponse =
                    objectMapper.readValue(requestBody, LineMessageWebhookResponse.class);
            if (webhookResponse.getEvents() != null) {
                for (Event event : webhookResponse.getEvents()) {
                    handleWebhookEvent(event);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    private void handleWebhookEvent(Event event) throws Exception {
        if (event == null || event.getSource() == null || StringUtils.isBlank(event.getSource().getUserId())) {
            return;
        }

        String lineUserId = event.getSource().getUserId();
        if ("postback".equalsIgnoreCase(event.getType())) {
            if (event.getPostback() == null || StringUtils.isBlank(event.getPostback().getData())) {
                return;
            }
            approvalService.handleLinePostback(lineUserId, event.getWebhookEventId(), event.getPostback().getData());
            return;
        }

        Message message = event.getMessage();
        if (!"message".equalsIgnoreCase(event.getType()) || message == null || StringUtils.isBlank(message.getText())) {
            return;
        }

        UserDto userDto = userProfileService.getUserByLineUserId(lineUserId);
        log.info("Receive message {} from user {} with {}", message.getText(), lineUserId, userDto.getId());
        lineHandleMessageService.handleTextMessage(userDto.getId(), message.getText());
    }

    @GetMapping("/v1/line/test")
    public GeneralResponse testLineConnect(@RequestParam("userId") String userId) throws Exception {
        log.info("=== Start test line connect ===");

        lineMessageService.sendTextMessage(userId, "ทดสอบการแจ้งเตือน");

        log.info("=== End test line connect ===");
        return new GeneralResponse(SUCCESS);
    }

    @PostMapping("/approval/test")

    public ResponseEntity<Void> sendTestApproval(

            @RequestBody SendApprovalRequest request

    ) throws Exception {

        lineMessageService.sendApprovalCard(

                request.lineUserId(),

                new ApprovalMessageData(

                        1001L,

                        "QT-20260710-001",

                        "NMT Limited",

                        "152,000.00 บาท"

                )

        );

        return ResponseEntity.noContent().build();

    }

    public record SendApprovalRequest(

            String lineUserId

    ) {

    }

}
