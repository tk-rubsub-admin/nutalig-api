package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.dto.line.Msg;
import com.nutalig.dto.line.SendMessageRequest;
import com.nutalig.entity.KeywordMappingPromptEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.registry.PromptResultDispatcher;
import com.nutalig.repository.KeywordMappingPromptRepository;
import com.nutalig.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineHandleMessageService {

    private final AiExecutionService aiExecutionService;
    private final KeywordMappingPromptRepository KeywordMappingPromptRepository;
    private final PromptResultDispatcher promptResultDispatcher;

    @Async("lineExecutor")
    public CompletableFuture<Void> handleTextMessage(String userId, String message) {
        try {
            String[] lines = message.split("\\r?\\n");
            log.info("Message have {} lines", lines.length);

            String keyword = lines[0].trim();
            Optional<KeywordMappingPromptEntity> opt =
                    KeywordMappingPromptRepository.findById(keyword);

            if (opt.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            KeywordMappingPromptEntity mappingPrompt = opt.get();
            String promptCode = mappingPrompt.getPromptCode();
            log.info("Found AI prompt with code {}", promptCode);

            String json = aiExecutionService.execute(promptCode, Map.of("message", message));
            String cleaned = extractJsonObject(json);

            promptResultDispatcher.dispatch(promptCode, userId, cleaned);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("handleTextMessage async error", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String extractJsonObject(String raw) {
        String s = raw.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s; // fallback
    }
}
