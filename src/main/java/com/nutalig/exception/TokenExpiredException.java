package com.nutalig.exception;

import com.nutalig.constant.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TokenExpiredException extends BaseCheckedException {

    public TokenExpiredException(String message) {
        this(ErrorCode.TOKEN_EXPIRED, message, HttpStatus.FORBIDDEN);
    }

    public TokenExpiredException(String code, String message) {
        this(code, message, HttpStatus.FORBIDDEN);
    }

    public TokenExpiredException(String code, String message, HttpStatus httpStatus) {
        super(code, message, httpStatus);
    }
}
