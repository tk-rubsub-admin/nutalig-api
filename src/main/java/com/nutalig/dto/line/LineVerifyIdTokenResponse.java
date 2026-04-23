package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LineVerifyIdTokenResponse {

    private String iss;
    private String sub;
    private String aud;
    private Long exp;
    private Long iat;
    private String nonce;
    private String name;
    private String picture;
    private String email;

    @JsonProperty("amr")
    private String[] authenticationMethods;
}
