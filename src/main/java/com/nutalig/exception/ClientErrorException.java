package com.nutalig.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatusCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ClientErrorException extends RuntimeException {
    private final String code;
    private final String message;
    private final HttpStatusCode httpStatus;
}
