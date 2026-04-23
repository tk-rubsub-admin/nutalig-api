package com.nutalig.controller.auth.response;

import com.nutalig.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineLoginResponse {

    private String accessToken;
    private UserDto user;
}
