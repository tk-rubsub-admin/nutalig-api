package com.nutalig.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseCheckedException extends Exception {
    protected final String code;
    protected final HttpStatus httpStatus;

    protected BaseCheckedException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

}
