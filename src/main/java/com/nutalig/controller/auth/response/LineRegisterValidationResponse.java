package com.nutalig.controller.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineRegisterValidationResponse {

    private boolean valid;
    private String status;
    private String username;
    private String displayName;
    private String email;
    private String invitedAt;
    private String inviteExpiresAt;
    private String registeredAt;
    private String message;
}
