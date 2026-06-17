package com.nutalig.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "template")
public class TemplateProperties {

    private Map<String, String> texts = new LinkedHashMap<>();
}
