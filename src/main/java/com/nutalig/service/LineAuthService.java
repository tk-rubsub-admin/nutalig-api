package com.nutalig.service;

import com.nutalig.config.LineConfiguration;
import com.nutalig.constant.Status;
import com.nutalig.controller.auth.request.LineLoginRequest;
import com.nutalig.controller.auth.request.LineRegisterRequest;
import com.nutalig.controller.auth.response.*;
import com.nutalig.dto.UserDto;
import com.nutalig.dto.line.LineStatePayload;
import com.nutalig.dto.line.LineTokenResponse;
import com.nutalig.dto.line.LineProfileResponse;
import com.nutalig.dto.line.LineVerifyAccessTokenResponse;
import com.nutalig.entity.UserEntity;
import com.nutalig.entity.UserRoleEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRepository;
import com.nutalig.repository.UserRoleRepository;
import com.nutalig.security.JwtUtil;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineAuthService {

    private static final String INTENT_LOGIN = "login";
    private static final String INTENT_REGISTER = "register";
    private static final String DEFAULT_REGISTER_ROLE_CODE = "USER";
    private static final String LINE_REGISTER_ACTOR = "LINE_REGISTER";
    private static final long STATE_EXPIRATION_SECONDS = 10 * 60;

    private final LineConfiguration lineConfiguration;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public LineLoginResponse login(LineLoginRequest request) throws InvalidRequestException, DataNotFoundException {
        UserDto user = getAuthenticatedUserByAccessToken(request.getAccessToken());
        return new LineLoginResponse(request.getAccessToken(), user);
    }

    public LineAuthorizeUrlResponse buildLoginAuthorizeUrl() throws InvalidRequestException {
        return new LineAuthorizeUrlResponse(buildAuthorizeUrl(LineStatePayload.builder()
                .intent(INTENT_LOGIN)
                .nonce(randomToken())
                .build()));
    }

    public LineAuthorizeUrlResponse buildRegisterAuthorizeUrl(String request) throws InvalidRequestException {
        return new LineAuthorizeUrlResponse(buildAuthorizeUrl(LineStatePayload.builder()
                .intent(INTENT_REGISTER)
                .userId(request)
                .nonce(randomToken())
                .build()));
    }

    public URI handleCallback(String code, String state, String error, String errorDescription)
            throws InvalidRequestException, DataNotFoundException {
        if (StringUtils.isNotBlank(error)) {
            return buildFailureRedirect(state, errorDescriptionOrDefault(error, errorDescription));
        }

        if (StringUtils.isBlank(code) || StringUtils.isBlank(state)) {
            throw new InvalidRequestException("Missing LINE authorization code or state");
        }

        LineStatePayload payload = parseState(state);
        LineTokenResponse tokenResponse = exchangeCodeForToken(code);
        String intent = payload.getIntent();

        if (INTENT_LOGIN.equals(intent)) {
            UserDto user = getAuthenticatedUserByAccessToken(tokenResponse.getAccessToken());
            return buildSuccessRedirect(lineConfiguration.getLoginSuccessUrl(),
                    Map.of(
                            "status", "success",
                            "mode", INTENT_LOGIN,
                            "access_token", tokenResponse.getAccessToken(),
                            "userId", user.getId()
                    ));
        }

        if (INTENT_REGISTER.equals(intent)) {
            RegisteredLineUser registeredLineUser = registerOrLoadUser(tokenResponse.getAccessToken(), payload.getUserId());
            return buildSuccessRedirect(lineConfiguration.getLoginSuccessUrl(),
                    Map.of(
                            "status", "success",
                            "mode", INTENT_REGISTER,
                            "access_token", tokenResponse.getAccessToken(),
                            "userId", registeredLineUser.user().getId(),
                            "created", String.valueOf(registeredLineUser.created())
                    ));
        }

        throw new InvalidRequestException("Unsupported LINE login intent");
    }

    public UserDto getAuthenticatedUserByAccessToken(String accessToken) throws InvalidRequestException, DataNotFoundException {
        String lineUserId = resolveLineUserIdFromAccessToken(accessToken);
        return userDetailsService.getUserByLineUserId(lineUserId);
    }

    private String buildAuthorizeUrl(LineStatePayload payload) throws InvalidRequestException {
        validateChannelConfiguration();

        String state = JwtUtil.generateToken(
                payload.getIntent(),
                Map.of(
                        "intent", payload.getIntent(),
                        "userId", StringUtils.defaultString(payload.getUserId()),
                        "nonce", payload.getNonce()
                ),
                STATE_EXPIRATION_SECONDS
        );

        return UriComponentsBuilder
                .fromHttpUrl(lineConfiguration.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", lineConfiguration.getLineChannelId())
                .queryParam("redirect_uri", lineConfiguration.getRedirectUri())
                .queryParam("state", state)
                .queryParam("scope", lineConfiguration.getScope())
                .queryParam("nonce", payload.getNonce())
                .encode()
                .build()
                .toUriString();
    }

    private LineStatePayload parseState(String state) throws InvalidRequestException {
        if (!JwtUtil.isValid(state)) {
            throw new InvalidRequestException("Invalid LINE state");
        }

        String intent = JwtUtil.getClaim(state, "intent");
        String userId = JwtUtil.getClaim(state, "userId");
        String nonce = JwtUtil.getClaim(state, "nonce");

        if (StringUtils.isBlank(intent) || StringUtils.isBlank(nonce)) {
            throw new InvalidRequestException("Invalid LINE state payload");
        }

        return LineStatePayload.builder()
                .intent(intent)
                .userId(StringUtils.trimToNull(userId))
                .nonce(nonce)
                .build();
    }

    private LineTokenResponse exchangeCodeForToken(String code) throws InvalidRequestException {
        validateChannelConfiguration();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", lineConfiguration.getRedirectUri());
        body.add("client_id", lineConfiguration.getLineChannelId());
        body.add("client_secret", lineConfiguration.getLineChannelSecret());

        try {
            ResponseEntity<LineTokenResponse> response = restTemplate.exchange(
                    lineConfiguration.getTokenUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    LineTokenResponse.class
            );

            if (response.getBody() == null || StringUtils.isBlank(response.getBody().getAccessToken())) {
                throw new InvalidRequestException("LINE token exchange failed");
            }

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("LINE token exchange failed: {}", e.getResponseBodyAsString());
            throw new InvalidRequestException("LINE token exchange failed");
        }
    }

    private String resolveLineUserIdFromAccessToken(String accessToken) throws InvalidRequestException {
        verifyAccessToken(accessToken);
        LineProfileResponse profile = getProfile(accessToken);

        if (StringUtils.isBlank(profile.getUserId())) {
            throw new InvalidRequestException("LINE profile does not contain userId");
        }

        return profile.getUserId();
    }

    private LineProfileResponse resolveProfileFromAccessToken(String accessToken) throws InvalidRequestException {
        verifyAccessToken(accessToken);
        LineProfileResponse profile = getProfile(accessToken);

        if (StringUtils.isBlank(profile.getUserId())) {
            throw new InvalidRequestException("LINE profile does not contain userId");
        }

        return profile;
    }

    private RegisteredLineUser registerOrLoadUser(String accessToken, String userId) throws InvalidRequestException, DataNotFoundException {
        LineProfileResponse profile = resolveProfileFromAccessToken(accessToken);

        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        existingUser.setDisplayName(profile.getDisplayName());
        existingUser.setUpdatedBy(LINE_REGISTER_ACTOR);
        existingUser.setLineUserId(profile.getUserId());
        existingUser.setPictureUrl(StringUtils.trimToNull(profile.getPictureUrl()));
        existingUser.setIsVerified(Boolean.TRUE);
        existingUser.setVerifiedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

        userRepository.save(existingUser);
        return new RegisteredLineUser(userDetailsService.getUserById(existingUser.getId()), true);
    }

    private void verifyAccessToken(String accessToken) throws InvalidRequestException {
        RestTemplate restTemplate = new RestTemplate();
        String url = lineConfiguration.getVerifyAccessTokenUrl() + "?access_token=" + accessToken;

        try {
            ResponseEntity<LineVerifyAccessTokenResponse> response =
                    restTemplate.getForEntity(url, LineVerifyAccessTokenResponse.class);

            LineVerifyAccessTokenResponse body = response.getBody();
            if (body == null || body.getExpiresIn() == null || body.getExpiresIn() <= 0) {
                throw new InvalidRequestException("LINE access token is invalid");
            }

            String configuredChannelId = lineConfiguration.getLineChannelId();
            if (StringUtils.isNotBlank(configuredChannelId) && !configuredChannelId.equals(body.getClientId())) {
                throw new InvalidRequestException("LINE access token was issued for another channel");
            }
        } catch (HttpStatusCodeException e) {
            log.warn("LINE access token verification failed: {}", e.getResponseBodyAsString());
            throw new InvalidRequestException("LINE access token is invalid");
        }
    }

    private LineProfileResponse getProfile(String accessToken) throws InvalidRequestException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<LineProfileResponse> response = restTemplate.exchange(
                    lineConfiguration.getProfileUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    LineProfileResponse.class
            );

            if (response.getBody() == null) {
                throw new InvalidRequestException("Cannot get LINE profile");
            }

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("LINE profile lookup failed: {}", e.getResponseBodyAsString());
            throw new InvalidRequestException("Cannot get LINE profile");
        }
    }

    private URI buildFailureRedirect(String state, String message) throws InvalidRequestException {
        String failureUrl = lineConfiguration.getLoginFailureUrl();

        if (StringUtils.isBlank(failureUrl)) {
            throw new InvalidRequestException(message);
        }

        return UriComponentsBuilder.fromUriString(failureUrl)
                .queryParam("status", "failed")
                .queryParam("message", message)
                .encode()
                .build()
                .toUri();
    }

    private URI buildSuccessRedirect(String baseUrl, Map<String, String> queryParams) throws InvalidRequestException {
        if (StringUtils.isBlank(baseUrl)) {
            throw new InvalidRequestException("LINE redirect URL is not configured");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        queryParams.forEach(builder::queryParam);
        log.info("URL : {}", builder.encode().build().toUri());
        return builder.encode().build().toUri();
    }

    private void validateChannelConfiguration() throws InvalidRequestException {
        if (StringUtils.isAnyBlank(
                lineConfiguration.getAuthorizeUrl(),
                lineConfiguration.getTokenUrl(),
                lineConfiguration.getLineChannelId(),
                lineConfiguration.getLineChannelSecret(),
                lineConfiguration.getRedirectUri()
        )) {
            throw new InvalidRequestException("LINE Login configuration is incomplete");
        }
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String errorDescriptionOrDefault(String error, String errorDescription) {
        return StringUtils.defaultIfBlank(errorDescription, error);
    }

    private record RegisteredLineUser(UserDto user, boolean created) {
    }
}
