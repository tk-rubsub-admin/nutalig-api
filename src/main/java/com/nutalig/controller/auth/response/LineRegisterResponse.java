package com.nutalig.controller.auth.response;

import com.nutalig.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineRegisterResponse {

    private String accessToken;
    private UserDto user;
    private boolean created;
}
