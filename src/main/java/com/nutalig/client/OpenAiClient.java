package com.nutalig.client;

import com.nutalig.dto.ai.AiRequest;
import com.nutalig.dto.ai.AiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;

    public AiResponse call(AiRequest request) {

        Map<String, Object> body = Map.of(
                "model", request.getModel(),
                "temperature", request.getTemperature(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.getSystemPrompt()),
                        Map.of("role", "user", "content", request.getUserPrompt())
                )
        );

        Map response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map choice = ((List<Map>) response.get("choices")).get(0);
        Map message = (Map) choice.get("message");

        String content = (String) message.get("content");

        Map usage = (Map) response.get("usage");

        return AiResponse.builder()
                .content(content)
                .inputTokens((Integer) usage.get("prompt_tokens"))
                .outputTokens((Integer) usage.get("completion_tokens"))
                .build();
    }
}
