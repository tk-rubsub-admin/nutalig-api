package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LineVerifyAccessTokenResponse {

    private String scope;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
