package com.nutalig.exception;

import com.nutalig.controller.response.GeneralResponse;
import org.springframework.http.HttpStatusCode;

public class DefaultClientErrorHandler implements ClientErrorHandler {

    @Override
    public <T> void handleError(HttpStatusCode statusCode, GeneralResponse<T> responseBody) {
        throw new ClientErrorException(
                responseBody.getStatus(),
                responseBody.getMessage(),
                statusCode
        );
    }

}

