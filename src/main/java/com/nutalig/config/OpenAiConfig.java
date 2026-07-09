package com.nutalig.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class OpenAiConfig {

    private final OpenAiProperties openAiProperties;

    @PostConstruct
    public void validate() {
        String apiKey = normalizedApiKey();

        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is blank after property binding");
        }

        log.info("OpenAI configuration loaded. baseUrl={}, apiKeyLength={}, apiKeyPrefix={}",
                openAiProperties.getBaseUrl(),
                apiKey.length(),
                maskedPrefix(apiKey));
    }

    @Bean
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + normalizedApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String normalizedApiKey() {
        return openAiProperties.getApiKey() == null ? "" : openAiProperties.getApiKey().trim();
    }

    private String maskedPrefix(String apiKey) {
        return apiKey.length() <= 12 ? apiKey : apiKey.substring(0, 12);
    }
}
