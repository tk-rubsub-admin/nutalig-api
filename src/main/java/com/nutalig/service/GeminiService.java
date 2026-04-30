package com.nutalig.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.model = model;
    }

    public String ask(String prompt) {
        GenerateContentResponse response =
                client.models.generateContent(model, prompt, null);
        return response.text();
    }
}