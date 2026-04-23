package com.nutalig.controller.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineAuthorizeUrlResponse {

    private String authorizeUrl;
}
