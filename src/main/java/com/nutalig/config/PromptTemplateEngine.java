package com.nutalig.config;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateEngine {

    public String render(String template, Map<String, String> variables) {
        String result = template == null ? "" : template;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : entry.getValue()
            );
        }

        return result;
    }
}
