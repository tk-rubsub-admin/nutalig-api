package com.nutalig.controller.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteLineRegistrationResponse {

    private String token;
    private String inviteToken;
    private String inviteUrl;
    private String registrationUrl;
    private String url;
}
