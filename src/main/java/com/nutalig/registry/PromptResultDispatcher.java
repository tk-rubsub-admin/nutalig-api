package com.nutalig.registry;

import com.nutalig.handler.PromptResultHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PromptResultDispatcher {

    private final Map<String, PromptResultHandler> handlerMap;

    public PromptResultDispatcher(List<PromptResultHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PromptResultHandler::promptCode,
                        Function.identity()
                ));
    }

    public void dispatch(String promptCode, String userId, String cleanedJson) throws Exception {
        PromptResultHandler handler = handlerMap.get(promptCode);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for promptCode=" + promptCode);
        }
        handler.handle(userId, cleanedJson);
    }
}