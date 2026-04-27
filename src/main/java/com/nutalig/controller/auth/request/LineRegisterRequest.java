package com.nutalig.controller.auth.request;

import lombok.Data;

@Data
public class LineRegisterRequest {
    private String token;
    private String accessToken;
    private String idToken;
}
