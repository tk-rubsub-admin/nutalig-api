package com.nutalig.controller.auth;

import com.nutalig.controller.auth.request.LoginRequest;
import com.nutalig.controller.auth.request.LineLoginRequest;
import com.nutalig.controller.auth.request.LineRegisterRequest;
import com.nutalig.controller.auth.response.*;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.LineAuthService;
import com.nutalig.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserProfileService userProfileService;
    private final LineAuthService lineAuthService;
//    private final OneTimeTokenService oneTimeTokenService;

    @PostMapping("/v1/login")
    public GeneralResponse login(@RequestBody LoginRequest loginRequest) {
        userProfileService.onUserLogin(loginRequest.getUserId());
        return new GeneralResponse(SUCCESS);
    }

    @PostMapping("/v1/logout")
    public GeneralResponse logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        userProfileService.onUserLogout(extractBearerToken(authorizationHeader));
        return new GeneralResponse(SUCCESS);
    }

    @PostMapping("/v1/auth/line/login")
    public GeneralResponse<LineLoginResponse> loginWithLine(@Valid @RequestBody LineLoginRequest request)
            throws InvalidRequestException, DataNotFoundException {
        LineLoginResponse response = lineAuthService.login(request);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/auth/line/login/url")
    public GeneralResponse<LineAuthorizeUrlResponse> getLineLoginUrl() throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, lineAuthService.buildLoginAuthorizeUrl());
    }

    @GetMapping("/v1/auth/line/register/url")
    public GeneralResponse<LineAuthorizeUrlResponse> getLineRegisterUrl(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "userId", required = false) String userId
    ) throws InvalidRequestException, DataNotFoundException {
        if (token != null && !token.isBlank()) {
            return new GeneralResponse<>(SUCCESS, lineAuthService.buildRegisterAuthorizeUrlByToken(token));
        }

        return new GeneralResponse<>(SUCCESS, lineAuthService.buildRegisterAuthorizeUrl(userId));
    }

    @GetMapping("/v1/auth/line/register/validate")
    public GeneralResponse<LineRegisterValidationResponse> validateLineRegisterToken(
            @RequestParam("token") String token
    ) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, lineAuthService.validateRegisterToken(token));
    }

    @PostMapping("/v1/auth/line/register")
    public GeneralResponse<LineRegisterResponse> registerWithLine(@Valid @RequestBody LineRegisterRequest request)
            throws InvalidRequestException, DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, lineAuthService.register(request));
    }

    @GetMapping("/v1/auth/line/callback")
    public ResponseEntity<Void> handleLineCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription
    ) throws InvalidRequestException, DataNotFoundException {
        URI redirectUri = lineAuthService.handleCallback(code, state, error, errorDescription);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

//    @PostMapping("/v1/auth/one-time-login")
//    public String oneTimeLogin(@RequestBody OneTimeTokenRequest oneTimeTokenRequest)
//            throws InvalidRequestException {
//        log.info("=== Start one time login ===");
//
//        String token = oneTimeTokenService.oneTimeLogin(oneTimeTokenRequest);
//
//        log.info("=== End one time login ===");
//        return token;
//    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

}
