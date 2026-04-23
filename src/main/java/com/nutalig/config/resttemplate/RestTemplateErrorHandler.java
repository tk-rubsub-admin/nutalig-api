package com.nutalig.config.resttemplate;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class RestTemplateErrorHandler implements ResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        // Do nothing
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return false;
    }
}
