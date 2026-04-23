package com.nutalig.controller.auth.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class LineLoginRequest {

    private String accessToken;

    @AssertTrue(message = "accessToken is required")
    public boolean isValidRequest() {
        return StringUtils.isNotBlank(accessToken);
    }
}
