package com.nutalig.service;

import com.nutalig.entity.AiPromptEntity;
import com.nutalig.exception.AiException;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.repository.AiPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final AiPromptRepository aiPromptRepository;

    public AiPromptEntity getActivePrompt(String code) throws DataNotFoundException {
        log.info("Get ai active prompt for {}", code);
        return aiPromptRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new DataNotFoundException("Prompt " + code + " not found."));
    }
}
