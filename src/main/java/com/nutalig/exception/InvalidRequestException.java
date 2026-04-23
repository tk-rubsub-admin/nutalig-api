package com.nutalig.exception;

import com.nutalig.constant.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidRequestException extends BaseCheckedException {

    public InvalidRequestException(String message) {
        this(ErrorCode.INVALID_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    public InvalidRequestException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public InvalidRequestException(String code, String message, HttpStatus httpStatus) {
        super(code, message, httpStatus);
    }
}
