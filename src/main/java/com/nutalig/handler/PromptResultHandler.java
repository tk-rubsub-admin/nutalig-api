package com.nutalig.handler;

public interface PromptResultHandler {
    String promptCode();
    void handle(String userId, String cleanedJson) throws Exception;
}