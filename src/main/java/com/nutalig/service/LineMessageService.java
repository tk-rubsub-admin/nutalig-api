package com.nutalig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.HashMap;
import java.util.ArrayList;
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

        if (StringUtils.isEmpty(userEntity.getLineUserId()) && !isTestEnvironment()) {
            throw new InvalidRequestException("User " + userId + " doesn't have line user id");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + lineConfiguration.getLineMessageAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setTo(resolveRecipient(userEntity.getLineUserId()));
        sendMessageRequest.setMessages(List.of(new Msg("text", message)));

        // สร้าง RequestEntity สำหรับ HTTP POST
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(sendMessageRequest), headers);

        // ส่ง POST request ไปที่ Line API
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        log.info("Response from Line API: " + response.getBody());
    }

    public void sendNotificationCard(
            String userId,
            String altText,
            String title,
            String detail,
            String detailUrl
    ) throws Exception {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found"));
        if (StringUtils.isBlank(userEntity.getLineUserId()) && !isTestEnvironment()) {
            throw new InvalidRequestException("User " + userId + " doesn't have line user id");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("altText", altText);
        placeholders.put("title", title);
        placeholders.put("detail", detail);
        placeholders.put("detailUrl", detailUrl);
        sendFlexMessage(userEntity.getLineUserId(), renderNotificationTemplate(placeholders));
    }

    public void sendTestNotificationCard(String userId) throws Exception {
        log.info("Send test notification card to user {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found"));

        if (StringUtils.isBlank(userEntity.getLineUserId()) && !isTestEnvironment()) {
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

        LinePushMessageRequest redirectedRequest = new LinePushMessageRequest(
                resolveRecipient(request.to()),
                request.messages()
        );
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(redirectedRequest), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        log.info("Response from Line API for approval card: {}", response.getBody());
    }

    private boolean isTestEnvironment() {
        return StringUtils.equalsIgnoreCase("test", StringUtils.trimToEmpty(lineConfiguration.getLineMessageEnv()));
    }

    private String resolveRecipient(String requestedLineUserId) throws InvalidRequestException {
        if (!isTestEnvironment()) {
            return requestedLineUserId;
        }
        String testUser = StringUtils.trimToNull(lineConfiguration.getLineMessageTestUser());
        if (testUser == null) {
            throw new InvalidRequestException("line.message.test-user is required when line.message.env is test.");
        }
        log.info("LINE test environment: redirecting message to configured test user.");
        return testUser;
    }

    private JsonNode loadApprovalCardTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource("line/approval-card.json");

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    JsonNode renderNotificationTemplate(Map<String, String> placeholders) throws Exception {
        ClassPathResource resource = new ClassPathResource("line/notification.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode template = objectMapper.readTree(inputStream);
            replacePlaceholders(template, placeholders);
            return template;
        }
    }

    private void replacePlaceholders(JsonNode node, Map<String, String> placeholders) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            node.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                if (child.isTextual()) {
                    objectNode.put(fieldName, replacePlaceholderText(child.asText(), placeholders));
                } else {
                    replacePlaceholders(child, placeholders);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int index = 0; index < arrayNode.size(); index++) {
                JsonNode child = arrayNode.get(index);
                if (child.isTextual()) {
                    arrayNode.set(index, objectMapper.getNodeFactory().textNode(
                            replacePlaceholderText(child.asText(), placeholders)));
                } else {
                    replacePlaceholders(child, placeholders);
                }
            }
        }
    }

    private String replacePlaceholderText(String text, Map<String, String> placeholders) {
        String resolved = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("${" + entry.getKey() + "}",
                    StringUtils.defaultString(entry.getValue()));
        }
        return resolved;
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
