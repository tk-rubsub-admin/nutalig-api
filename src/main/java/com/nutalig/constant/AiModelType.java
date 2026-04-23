package com.nutalig.constant;

public enum AiModelType {
    GPT_4_1_MINI("gpt-4.1-mini"),
    GPT_4_1("gpt-4.1");

    private final String modelName;

    AiModelType(String modelName) {
        this.modelName = modelName;
    }
}
