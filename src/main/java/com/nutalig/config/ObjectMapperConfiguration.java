package com.nutalig.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nutalig.utils.DateUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.ZonedDateTime;

@Configuration
public class ObjectMapperConfiguration {

    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        final SimpleModule zonedDateTimeSerializer = new SimpleModule();
        zonedDateTimeSerializer.addSerializer(ZonedDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(ZonedDateTime zonedDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeString(DateUtil.YYYY_MM_DD_HH_MM_SS_SSSZ.format(zonedDateTime.withZoneSameInstant(DateUtil.getTimeZone())));
            }
        });

        objectMapper.registerModule(zonedDateTimeSerializer);
        return objectMapper;
    }

}
