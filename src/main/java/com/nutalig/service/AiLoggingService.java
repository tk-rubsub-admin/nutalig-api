package com.nutalig.service;

import com.nutalig.dto.ai.AiResponse;
import com.nutalig.entity.AiPromptLogEntity;
import com.nutalig.repository.AiPromptLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLoggingService {

    private final AiPromptLogRepository aiPromptLogRepository;

    public void logPrompt(String promptCode, AiResponse aiResponse, long responseTimeMs) {
        log.info("Add log prompt {}", promptCode);

        Double cost = estimateCost(aiResponse.getInputTokens(), aiResponse.getOutputTokens());

        AiPromptLogEntity log = new AiPromptLogEntity();

        log.setPromptCode(promptCode);
        log.setInputTokens(aiResponse.getInputTokens());
        log.setOutputTokens(aiResponse.getOutputTokens());
        log.setCostEstimate(cost);
        log.setResponseTimeMs(responseTimeMs);

        aiPromptLogRepository                                                                                                                                                                                                                    .save(log);
    }

    private Double estimateCost(int input, int output) {
        return (input * 0.0000004) + (output * 0.0000016);
    }
}
