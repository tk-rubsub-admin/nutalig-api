package com.nutalig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalTemplateService {

    private final ObjectMapper objectMapper;

    public JsonNode renderTemplate(String templateCode, Map<String, String> placeholders) throws Exception {
        String template = loadTemplate(templateCode);
        String rendered = template;

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rendered = rendered.replace(
                        "${" + entry.getKey() + "}",
                        StringUtils.defaultString(entry.getValue())
                );
            }
        }

        return objectMapper.readTree(rendered);
    }

    private String loadTemplate(String templateCode) throws Exception {
        ClassPathResource resource = new ClassPathResource("line/approval-templates/" + templateCode + ".json");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
