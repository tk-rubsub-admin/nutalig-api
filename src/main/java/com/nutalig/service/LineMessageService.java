package com.nutalig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.dto.line.ApprovalMessageData;
import com.nutalig.dto.line.LinePushMessageRequest;
import com.nutalig.dto.line.Msg;
import com.nutalig.dto.line.SendMessageRequest;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineMessageService {

    private final UserRepository userRepository;
    private final LineConfiguration lineConfiguration;
    private final ObjectMapper objectMapper;

    public void sendTextMessage(String userId, String message) throws Exception {
        log.info("Send message {} to user {}", message, userId);
        String url = lineConfiguration.getLineMessageApiUrl();

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found"));

        if (StringUtils.isEmpty(userEntity.getLineUserId())) {
            throw new InvalidRequestException("User " + userId + " doesn't have line user id");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + lineConfiguration.getLineMessageAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setTo(userEntity.getLineUserId());
        sendMessageRequest.setMessages(List.of(new Msg("text", message)));

        // สร้าง RequestEntity สำหรับ HTTP POST
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(sendMessageRequest), headers);

        // ส่ง POST request ไปที่ Line API
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        log.info("Response from Line API: " + response.getBody());
    }

    public void sendTestNotificationCard(String userId) throws Exception {
        log.info("Send test notification card to user {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found"));

        if (StringUtils.isBlank(userEntity.getLineUserId())) {
            throw new InvalidRequestException("User " + userId + " doesn't have line user id");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("altText", "ทดสอบการแจ้งเตือนจากระบบ");
        placeholders.put("title", "ทดสอบการแจ้งเตือน");
        placeholders.put(
                "detail",
                String.format(
                        "สวัสดี %s นี่คือข้อความทดสอบการแจ้งเตือนจากระบบ NUTALIG",
                        StringUtils.defaultIfBlank(userEntity.getDisplayName(), userEntity.getUsername())
                )
        );
        placeholders.put("detailUrl", buildTestNotificationDetailUrl());

        JsonNode message = renderNotificationTemplate(placeholders);
        sendFlexMessage(userEntity.getLineUserId(), message);
    }

    public void sendTextMessageToLineUser(String lineUserId, String message) throws Exception {
        log.info("Send message {} to line user {}", message, lineUserId);
        sendPushMessage(new LinePushMessageRequest(
                lineUserId,
                List.of(new LinePushMessageRequest.LineMessage("text", message, null, null))
        ));
    }

    public void sendFlexMessage(String lineUserId, JsonNode messageTemplate) throws Exception {
        LinePushMessageRequest.LineMessage message =
                new LinePushMessageRequest.LineMessage(
                        messageTemplate.path("type").asText("flex"),
                        null,
                        messageTemplate.path("altText").asText("มีรายการรอการอนุมัติ"),
                        messageTemplate.path("contents")
                );
        sendPushMessage(new LinePushMessageRequest(lineUserId, List.of(message)));
    }

    public void sendApprovalCard(
            String lineUserId,
            ApprovalMessageData data
    ) throws Exception {
        log.info("Send approval card template to line user {}", lineUserId);

        String url = lineConfiguration.getLineMessageApiUrl();

        JsonNode template = loadApprovalCardTemplate();

        LinePushMessageRequest.LineMessage message =
                new LinePushMessageRequest.LineMessage(
                        template.path("type").asText("flex"),
                        null,
                        template.path("altText").asText("มีรายการรอการอนุมัติ"),
                        template.path("contents")
                );

        LinePushMessageRequest request =
                new LinePushMessageRequest(
                        lineUserId,
                        List.of(message)
                );

        sendPushMessage(request);
    }

    private void sendPushMessage(LinePushMessageRequest request) throws Exception {
        String url = lineConfiguration.getLineMessageApiUrl();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + lineConfiguration.getLineMessageAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        log.info("Response from Line API for approval card: {}", response.getBody());
    }

    private JsonNode loadApprovalCardTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource("line/approval-card.json");

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    private JsonNode renderNotificationTemplate(Map<String, String> placeholders) throws Exception {
        ClassPathResource resource = new ClassPathResource("line/notification.json");
        try (InputStream inputStream = resource.getInputStream()) {
            String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String rendered = template;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rendered = rendered.replace("${" + entry.getKey() + "}", StringUtils.defaultString(entry.getValue()));
            }
            return objectMapper.readTree(rendered);
        }
    }

    private String buildTestNotificationDetailUrl() throws InvalidRequestException {
        String loginSuccessUrl = lineConfiguration.getLoginSuccessUrl();
        if (StringUtils.isBlank(loginSuccessUrl)) {
            throw new InvalidRequestException("LINE frontend redirect URL is not configured");
        }

        URI uri = URI.create(loginSuccessUrl);
        String frontendBaseUrl = uri.getScheme() + "://" + uri.getAuthority();
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/")
                .build()
                .toUriString();
    }
}
