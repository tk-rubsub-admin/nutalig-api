package com.nutalig.controller.auth.request;

import com.nutalig.constant.AppSessionDeviceType;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class LineRegisterRequest {
    private String token;
    private String accessToken;
    private String idToken;
    private AppSessionDeviceType deviceType;

    @AssertTrue(message = "token, accessToken and deviceType are required")
    public boolean isValidRequest() {
        return StringUtils.isNotBlank(token)
                && StringUtils.isNotBlank(accessToken)
                && deviceType != null;
    }
}
