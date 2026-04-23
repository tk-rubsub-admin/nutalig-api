package com.nutalig.dto.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiRequest {

    private String model;
    private Double temperature;
    private String systemPrompt;
    private String userPrompt;
}
