package com.nutalig.exception;

import com.nutalig.controller.response.GeneralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;

@Slf4j
public class SilentClientErrorHandler implements ClientErrorHandler {

    @Override
    public <T> void handleError(HttpStatusCode statusCode, GeneralResponse<T> responseBody) {
        log.warn("Http status {}\nresponse status {}\nmessage {}", statusCode, responseBody.getStatus(), responseBody.getMessage());
    }

}

