package com.nutalig.controller.line;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.controller.line.response.LineMessageWebhookResponse;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.UserDto;
import com.nutalig.service.LineHandleMessageService;
import com.nutalig.service.LineMessageService;
import com.nutalig.service.UserProfileService;
import com.nutalig.utils.LineSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    @PostMapping("/line-webhook")
    public ResponseEntity<String> handleLineWebhook(@RequestBody String requestBody, @RequestHeader("X-Line-Signature") String signature) {
        log.info("Handle Line webhook");
        boolean isValid = LineSignatureValidator.validate(
                lineConfiguration.getLineChannelSecret(),
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
            String userId = webhookResponse
                    .getEvents()
                    .getFirst()
                    .getSource()
                    .getUserId();
            String message = webhookResponse
                    .getEvents()
                    .getFirst()
                    .getMessage()
                    .getText();

            UserDto userDto = userProfileService.getUserByLineUserId(userId);

            log.info("Receive message {} from user {} with {}", message, userId, userDto.getId());
            lineHandleMessageService.handleTextMessage(userDto.getId(), message);
        } catch (Exception e) {
            log.error("Error parsing webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @GetMapping("/v1/line/test")
    public GeneralResponse testLineConnect(@RequestParam("userId") String userId) throws Exception {
        log.info("=== Start test line connect ===");

        lineMessageService.sendTextMessage(userId, "ทดสอบการแจ้งเตือน");

        log.info("=== End test line connect ===");
        return new GeneralResponse(SUCCESS);
    }

}
