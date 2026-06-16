package com.nutalig.service;

import com.nutalig.entity.KeywordMappingPromptEntity;
import com.nutalig.registry.PromptResultDispatcher;
import com.nutalig.repository.KeywordMappingPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

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

            if (lines.length == 0 || StringUtils.isBlank(lines[0])) {
                log.info("Skip line message because first line is blank");
                return CompletableFuture.completedFuture(null);
            }

            String firstLine = lines[0].trim();
            Optional<KeywordMappingPromptEntity> opt = StreamSupport.stream(
                            KeywordMappingPromptRepository.findAll().spliterator(), false)
                    .filter(mapping -> matchesPrefix(firstLine, mapping))
                    .max((left, right) -> Integer.compare(
                            prefixLength(right.getKeyword()),
                            prefixLength(left.getKeyword())
                    ));

            if (opt.isEmpty()) {
                log.info("Skip line message because no keyword mapping matched first line: {}", firstLine);
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

    private boolean matchesPrefix(String firstLine, KeywordMappingPromptEntity mapping) {
        if (mapping == null || StringUtils.isBlank(mapping.getKeyword())) {
            return false;
        }

        String prefix = normalizeKeywordPrefix(mapping.getKeyword());
        return StringUtils.isNotBlank(prefix) && firstLine.startsWith(prefix);
    }

    private String normalizeKeywordPrefix(String keyword) {
        String normalized = StringUtils.trimToEmpty(keyword);
        int wildcardIndex = normalized.indexOf('%');
        if (wildcardIndex >= 0) {
            normalized = normalized.substring(0, wildcardIndex);
        }
        return StringUtils.trimToEmpty(normalized);
    }

    private int prefixLength(String keyword) {
        return normalizeKeywordPrefix(keyword).length();
    }
}
