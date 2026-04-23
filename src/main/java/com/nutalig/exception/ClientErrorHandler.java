package com.nutalig.exception;

import com.nutalig.controller.response.GeneralResponse;
import org.springframework.http.HttpStatusCode;

public interface ClientErrorHandler {
    <T> void handleError(HttpStatusCode statusCode, GeneralResponse<T> body);

}
