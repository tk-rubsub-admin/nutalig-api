package com.nutalig.config.resttemplate;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.spring.web.TracingClientHttpRequestInterceptor;
import com.nutalig.interceptor.RestClientHeaderModifierInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfiguration {

    @Bean
    public HttpTracing create(Tracing tracing) {
        return HttpTracing
                .newBuilder(tracing)
                .build();
    }

    @Bean
    public RestClient restClient(RestClient.Builder restClientBuilder, HttpTracing httpTracing) {

        MappingJackson2HttpMessageConverter  mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));

        return restClientBuilder
                .requestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient(180)))
                .requestInterceptors(interceptors -> {
                    interceptors.add(new RestClientHeaderModifierInterceptor());
                    interceptors.add(TracingClientHttpRequestInterceptor.create(httpTracing));
                })
                .messageConverters(msgConverters -> {
                    msgConverters.add(new StringHttpMessageConverter());
                    msgConverters.add(mappingJackson2HttpMessageConverter);
                })
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                })
                .build();
    }
    private HttpClient getHttpClient(long readTimeout) {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(120))
                .setResponseTimeout(Timeout.ofSeconds(readTimeout))
                .build();

        final ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(120))
                .setSocketTimeout(Timeout.ofSeconds(300))
                .build();

        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(125);
        connectionManager.setDefaultMaxPerRoute(25);
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
