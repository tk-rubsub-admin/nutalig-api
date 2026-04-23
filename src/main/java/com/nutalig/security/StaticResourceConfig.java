package com.nutalig.security;

import com.nutalig.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(appProperties.getUpload().getDir())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        System.out.println("Static resource location = " + absolutePath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
}
