package com.nutalig.config;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void test() {
        texts.forEach((key, value) -> System.out.println("template." + key + " = " + value));
    }
}
