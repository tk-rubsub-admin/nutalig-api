package com.nutalig.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Upload upload = new Upload();

    @Getter
    @Setter
    public static class Upload {
        private String dir;
        private String publicBaseUrl;
    }
}
