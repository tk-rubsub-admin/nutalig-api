package com.nutalig.dto.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiResponse {

    private String content;
    private Integer inputTokens;
    private Integer outputTokens;
}