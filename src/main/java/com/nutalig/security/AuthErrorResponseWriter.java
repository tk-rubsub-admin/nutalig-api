package com.nutalig.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.controller.response.GeneralResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, int httpStatus, String code, String message)
            throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reason", code);
        data.put("path", request.getRequestURI());
        data.put("timestamp", ZonedDateTime.now());

        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                GeneralResponse.builder()
                        .status(code)
                        .message(message)
                        .data(data)
                        .build()
        ));
    }
}
