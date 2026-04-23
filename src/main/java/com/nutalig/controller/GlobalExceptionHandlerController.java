package com.nutalig.controller;

import com.nutalig.constant.ErrorCode;
import com.nutalig.constant.ResponseStatus;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.exception.BaseCheckedException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.exception.TokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerController {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponse<String>> handleGlobalException(Exception e) {
        final GeneralResponse generalResponse = GeneralResponse.builder()
                .status(ResponseStatus.FAILED)
                .message(e.getMessage())
                .build();
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(generalResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({BaseCheckedException.class})
    public ResponseEntity<GeneralResponse<String>> handleBaseCheckedException(BaseCheckedException e) {
        log.warn("Exception code : {}, message : {}", e.getCode(), e.getMessage());
        final GeneralResponse generalResponse = GeneralResponse.builder()
                .status(e.getCode())
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(generalResponse, e.getHttpStatus());
    }

    @ExceptionHandler({InvalidRequestException.class})
    public ResponseEntity<GeneralResponse<String>> handleInvalidRequestException(InvalidRequestException e) {
        String code = ResponseStatus.FAILED;

        if (e.getCode() != null) {
            code = e.getCode();
        }

        final GeneralResponse generalResponse = GeneralResponse.builder()
                .status(code)
                .message(e.getMessage())
                .build();
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(generalResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({TokenExpiredException.class})
    public ResponseEntity<GeneralResponse<String>> handleTokenExpiredException(TokenExpiredException e) {
        String code = ResponseStatus.FAILED;

        if (e.getCode() != null) {
            code = e.getCode();
        }

        final GeneralResponse generalResponse = GeneralResponse.builder()
                .status(code)
                .message(e.getMessage())
                .build();
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(generalResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError error = Optional.ofNullable(e.getFieldError()).orElseGet(() -> new FieldError(e.getObjectName(), "Unknown", "Invalid Request"));
        String errorMessage = String.format("[%s] parameter error. Description: [%s]", error.getField(), error.getDefaultMessage());

        final GeneralResponse generalResponse = GeneralResponse.builder().status(ErrorCode.INVALID_REQUEST).message(errorMessage).build();

        log.error("Method argument not valid exception: " + e.getMessage());
        return new ResponseEntity<>(generalResponse, HttpStatus.BAD_REQUEST);
    }


}
