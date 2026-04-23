package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.dto.line.Msg;
import com.nutalig.dto.line.SendMessageRequest;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
        headers.set("Authorization", "Bearer " + lineConfiguration.getLineChannelAccessToken());
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
}
