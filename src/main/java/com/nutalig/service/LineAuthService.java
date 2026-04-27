package com.nutalig.service;

import com.nutalig.config.LineConfiguration;
import com.nutalig.constant.Status;
import com.nutalig.controller.auth.request.LineLoginRequest;
import com.nutalig.controller.auth.request.LineRegisterRequest;
import com.nutalig.controller.auth.response.InviteLineRegistrationResponse;
import com.nutalig.controller.auth.response.*;
import com.nutalig.controller.auth.response.LineRegisterValidationResponse;
import com.nutalig.dto.UserDto;
import com.nutalig.dto.line.LineProfileResponse;
import com.nutalig.dto.line.LineStatePayload;
import com.nutalig.dto.line.LineTokenResponse;
import com.nutalig.dto.line.LineVerifyAccessTokenResponse;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRepository;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineAuthService {

    private static final String INTENT_LOGIN = "login";
    private static final String INTENT_REGISTER = "register";
    private static final String REGISTER_INVITE_INTENT = "register_invite";
    private static final String LINE_REGISTER_ACTOR = "LINE_REGISTER";
    private static final String SUPER_ADMIN_ACTOR = "USER-000001";
    private static final long STATE_EXPIRATION_SECONDS = 10 * 60;
    private static final long REGISTER_INVITE_EXPIRATION_SECONDS = 7 * 24 * 60 * 60;

    private static final String REGISTER_STATUS_READY = "READY";
    private static final String REGISTER_STATUS_REGISTERED = "REGISTERED";
    private static final String REGISTER_STATUS_EXPIRED = "EXPIRED";
    private static final String REGISTER_STATUS_INVALID = "INVALID";

    private final LineConfiguration lineConfiguration;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;

    public LineLoginResponse login(LineLoginRequest request) throws InvalidRequestException, DataNotFoundException {
        UserDto user = getAuthenticatedUserByAccessToken(request.getAccessToken());
        return new LineLoginResponse(request.getAccessToken(), user);
    }

    public LineRegisterResponse register(LineRegisterRequest request) throws InvalidRequestException, DataNotFoundException {
        if (request == null) {
            throw new InvalidRequestException("LINE register request is required");
        }
        if (StringUtils.isBlank(request.getToken())) {
            throw new InvalidRequestException("Registration token is required");
        }
        if (StringUtils.isBlank(request.getAccessToken())) {
            throw new InvalidRequestException("LINE access token is required");
        }

        RegistrationInviteContext context = validateRegistrationInvite(request.getToken());
        LineProfileResponse profile = resolveProfileFromAccessToken(request.getAccessToken());
        UserEntity user = context.user();

        if (StringUtils.isNotBlank(user.getLineUserId())) {
            if (StringUtils.equals(user.getLineUserId(), profile.getUserId())) {
                return new LineRegisterResponse(request.getAccessToken(), userDetailsService.getUserById(user.getId()), false);
            }
            throw new InvalidRequestException("บัญชีนี้ผูก LINE แล้ว");
        }

        UserEntity existingBoundUser = userRepository.findByLineUserId(profile.getUserId())
                .orElse(null);
        if (existingBoundUser != null && !StringUtils.equals(existingBoundUser.getId(), user.getId())) {
            throw new InvalidRequestException("LINE account นี้ถูกผูกกับผู้ใช้อื่นแล้ว");
        }

        bindLineProfile(user, profile, LINE_REGISTER_ACTOR);
        userRepository.save(user);

        return new LineRegisterResponse(request.getAccessToken(), userDetailsService.getUserById(user.getId()), true);
    }

    public LineAuthorizeUrlResponse buildLoginAuthorizeUrl() throws InvalidRequestException {
        return new LineAuthorizeUrlResponse(buildAuthorizeUrl(LineStatePayload.builder()
                .intent(INTENT_LOGIN)
                .nonce(randomToken())
                .build()));
    }

    public LineAuthorizeUrlResponse buildRegisterAuthorizeUrl(String userId) throws InvalidRequestException {
        return new LineAuthorizeUrlResponse(buildAuthorizeUrl(LineStatePayload.builder()
                .intent(INTENT_REGISTER)
                .userId(userId)
                .nonce(randomToken())
                .build()));
    }

    public LineAuthorizeUrlResponse buildRegisterAuthorizeUrlByToken(String token) throws InvalidRequestException, DataNotFoundException {
        RegistrationInviteContext context = validateRegistrationInvite(token);
        return new LineAuthorizeUrlResponse(buildAuthorizeUrl(LineStatePayload.builder()
                .intent(INTENT_REGISTER)
                .userId(context.user().getId())
                .registrationToken(token)
                .nonce(randomToken())
                .build()));
    }

    public LineRegisterValidationResponse validateRegisterToken(String token) throws DataNotFoundException {
        try {
            RegistrationInviteContext context = validateRegistrationInvite(token);
            UserEntity user = context.user();

            if (StringUtils.isNotBlank(user.getLineUserId())) {
                return buildValidationResponse(user, token, false, REGISTER_STATUS_REGISTERED, "บัญชีนี้ผูก LINE แล้ว");
            }

            return buildValidationResponse(user, token, true, REGISTER_STATUS_READY, "พร้อมลงทะเบียนผ่าน LINE");
        } catch (InvalidRequestException e) {
            String status = JwtUtil.isExpired(token) ? REGISTER_STATUS_EXPIRED : REGISTER_STATUS_INVALID;
            return buildInvalidValidationResponse(status, e.getMessage());
        }
    }

    public InviteLineRegistrationResponse inviteLineRegistration(String userId) throws DataNotFoundException, InvalidRequestException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        String token = JwtUtil.generateToken(
                user.getId(),
                Map.of(
                        "intent", REGISTER_INVITE_INTENT,
                        "userId", user.getId(),
                        "nonce", randomToken()
                ),
                REGISTER_INVITE_EXPIRATION_SECONDS
        );

        String inviteUrl = buildRegisterInviteUrl(token);
        return new InviteLineRegistrationResponse(token, token, inviteUrl, inviteUrl, inviteUrl);
    }

    public void resetLineBinding(String userId) throws DataNotFoundException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        user.setLineUserId(null);
        user.setUsername(null);
        user.setDisplayName(null);
        user.setPictureUrl(null);
        user.setIsVerified(Boolean.FALSE);
        user.setStatus(Status.PENDING_ACTIVATE);
        user.setVerifiedDate(null);
        user.setUpdatedBy(SUPER_ADMIN_ACTOR);
        userRepository.save(user);
    }

    public URI handleCallback(String code, String state, String error, String errorDescription)
            throws InvalidRequestException, DataNotFoundException {
        if (StringUtils.isNotBlank(error)) {
            return buildFailureRedirect(errorDescriptionOrDefault(error, errorDescription));
        }

        if (StringUtils.isBlank(code) || StringUtils.isBlank(state)) {
            throw new InvalidRequestException("Missing LINE authorization code or state");
        }

        LineStatePayload payload = parseState(state);
        LineTokenResponse tokenResponse = exchangeCodeForToken(code);
        String intent = payload.getIntent();

        if (INTENT_LOGIN.equals(intent)) {
            UserDto user = getAuthenticatedUserByAccessToken(tokenResponse.getAccessToken());
            return buildSuccessRedirect(
                    lineConfiguration.getLoginSuccessUrl(),
                    Map.of(
                            "status", "success",
                            "mode", INTENT_LOGIN,
                            "access_token", tokenResponse.getAccessToken(),
                            "userId", user.getId()
                    )
            );
        }

        if (INTENT_REGISTER.equals(intent)) {
            String registrationToken = StringUtils.defaultIfBlank(payload.getRegistrationToken(), payload.getUserId());
            return buildSuccessRedirect(
                    buildRegisterSuccessUrl(),
                    Map.of(
                            "status", "success",
                            "mode", INTENT_REGISTER,
                            "access_token", tokenResponse.getAccessToken(),
                            "id_token", StringUtils.defaultString(tokenResponse.getIdToken()),
                            "registrationToken", registrationToken,
                            "state", registrationToken
                    )
            );
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
                        "registrationToken", StringUtils.defaultString(payload.getRegistrationToken()),
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
        String registrationToken = JwtUtil.getClaim(state, "registrationToken");
        String nonce = JwtUtil.getClaim(state, "nonce");

        if (StringUtils.isBlank(intent) || StringUtils.isBlank(nonce)) {
            throw new InvalidRequestException("Invalid LINE state payload");
        }

        return LineStatePayload.builder()
                .intent(intent)
                .userId(StringUtils.trimToNull(userId))
                .registrationToken(StringUtils.trimToNull(registrationToken))
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

    private RegistrationInviteContext validateRegistrationInvite(String token) throws InvalidRequestException, DataNotFoundException {
        if (StringUtils.isBlank(token)) {
            throw new InvalidRequestException("Registration token is required");
        }
        if (JwtUtil.isExpired(token)) {
            throw new InvalidRequestException("ลิงก์ลงทะเบียนหมดอายุแล้ว");
        }
        if (!JwtUtil.isValid(token)) {
            throw new InvalidRequestException("ลิงก์ลงทะเบียนไม่ถูกต้อง");
        }
        if (!REGISTER_INVITE_INTENT.equals(JwtUtil.getClaim(token, "intent"))) {
            throw new InvalidRequestException("ลิงก์ลงทะเบียนไม่ถูกต้อง");
        }

        String userId = StringUtils.defaultIfBlank(JwtUtil.getClaim(token, "userId"), JwtUtil.getSubject(token));
        if (StringUtils.isBlank(userId)) {
            throw new InvalidRequestException("ลิงก์ลงทะเบียนไม่ถูกต้อง");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        return new RegistrationInviteContext(user, token);
    }

    private void bindLineProfile(UserEntity user, LineProfileResponse profile, String actor) {
        user.setDisplayName(profile.getDisplayName());
        user.setUpdatedBy(actor);
        user.setLineUserId(profile.getUserId());
        user.setPictureUrl(StringUtils.trimToNull(profile.getPictureUrl()));
        user.setStatus(Status.ACTIVE);
        user.setIsVerified(Boolean.TRUE);
        user.setVerifiedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
    }

    private LineRegisterValidationResponse buildValidationResponse(
            UserEntity user,
            String token,
            boolean valid,
            String status,
            String message
    ) {
        return new LineRegisterValidationResponse(
                valid,
                status,
                user.getUsername(),
                user.getDisplayName(),
                user.getUsername(),
                formatDate(JwtUtil.getIssuedAt(token)),
                formatDate(JwtUtil.getExpiration(token)),
                user.getVerifiedDate() != null ? user.getVerifiedDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null,
                message
        );
    }

    private LineRegisterValidationResponse buildInvalidValidationResponse(String status, String message) {
        return new LineRegisterValidationResponse(false, status, null, null, null, null, null, null, message);
    }

    private URI buildFailureRedirect(String message) throws InvalidRequestException {
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
        return builder.encode().build().toUri();
    }

    private String buildRegisterInviteUrl(String token) throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/line-register")
                .queryParam("token", token)
                .encode()
                .build()
                .toUriString();
    }

    private String buildRegisterSuccessUrl() throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/line-register-success")
                .build()
                .toUriString();
    }

    private String buildFrontendBaseUrl() throws InvalidRequestException {
        String loginSuccessUrl = lineConfiguration.getLoginSuccessUrl();
        if (StringUtils.isBlank(loginSuccessUrl)) {
            throw new InvalidRequestException("LINE frontend redirect URL is not configured");
        }

        URI uri = URI.create(loginSuccessUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
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

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }

        return date.toInstant()
                .atZone(DateUtil.getTimeZone())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private record RegistrationInviteContext(UserEntity user, String token) {
    }
}
