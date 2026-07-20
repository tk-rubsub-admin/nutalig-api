package com.nutalig.service;

import com.nutalig.constant.AppSessionDeviceType;
import com.nutalig.constant.ErrorCode;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.UserAppSessionEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserAppSessionRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppSessionService {

    private static final String TOKEN_TYPE_SESSION = "app_session";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_SESSION_ID = "sessionId";
    private static final String CLAIM_DEVICE_TYPE = "deviceType";
    private static final long SESSION_EXPIRATION_SECONDS = 48 * 60 * 60;

    private final UserRepository userRepository;
    private final UserAppSessionRepository userAppSessionRepository;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public String issueSessionToken(UserEntity user, AppSessionDeviceType deviceType) {
        String sessionId = UUID.randomUUID().toString();
        ZonedDateTime expiresAt = ZonedDateTime.now().plusSeconds(SESSION_EXPIRATION_SECONDS);

        UserAppSessionEntity session = userAppSessionRepository.findByUser_IdAndDeviceType(user.getId(), deviceType)
                .orElseGet(UserAppSessionEntity::new);
        session.setUser(user);
        session.setDeviceType(deviceType);
        session.setSessionId(sessionId);
        session.setExpiresAt(expiresAt);
        userAppSessionRepository.save(session);

        return JwtUtil.generateToken(
                user.getId(),
                Map.of(
                        CLAIM_TOKEN_TYPE, TOKEN_TYPE_SESSION,
                        CLAIM_SESSION_ID, sessionId,
                        CLAIM_DEVICE_TYPE, deviceType.name()
                ),
                SESSION_EXPIRATION_SECONDS
        );
    }

    @Transactional
    public void revokeSession(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }

        userRepository.findById(userId.trim()).ifPresent(user -> {
            user.setCurrentSessionId(null);
            userRepository.save(user);
        });
        userAppSessionRepository.deleteByUser_Id(userId.trim());
    }

    @Transactional
    public void revokeSessionByToken(String token) {
        if (StringUtils.isBlank(token) || !JwtUtil.isValid(token)) {
            return;
        }

        String tokenType = JwtUtil.getClaim(token, CLAIM_TOKEN_TYPE);
        String userId = StringUtils.trimToNull(JwtUtil.getSubject(token));
        String sessionId = StringUtils.trimToNull(JwtUtil.getClaim(token, CLAIM_SESSION_ID));
        AppSessionDeviceType deviceType = parseDeviceType(JwtUtil.getClaim(token, CLAIM_DEVICE_TYPE));

        if (!TOKEN_TYPE_SESSION.equals(tokenType) || userId == null || sessionId == null || deviceType == null) {
            return;
        }

        userAppSessionRepository.deleteByUser_IdAndSessionIdAndDeviceType(userId, sessionId, deviceType);
    }

    @Transactional(readOnly = true)
    public UserDto authenticate(String token) throws InvalidRequestException, DataNotFoundException {
        if (StringUtils.isBlank(token)) {
            throw unauthorized(ErrorCode.INVALID_REQUEST, "Missing access token");
        }
        if (JwtUtil.isExpired(token)) {
            throw unauthorized(ErrorCode.TOKEN_EXPIRED, "Session expired");
        }
        if (!JwtUtil.isValid(token)) {
            throw unauthorized(ErrorCode.INVALID_REQUEST, "Invalid access token");
        }

        String tokenType = JwtUtil.getClaim(token, CLAIM_TOKEN_TYPE);
        if (!TOKEN_TYPE_SESSION.equals(tokenType)) {
            throw unauthorized(ErrorCode.INVALID_REQUEST, "Unsupported access token");
        }

        String userId = StringUtils.trimToNull(JwtUtil.getSubject(token));
        String sessionId = StringUtils.trimToNull(JwtUtil.getClaim(token, CLAIM_SESSION_ID));
        AppSessionDeviceType deviceType = parseDeviceType(JwtUtil.getClaim(token, CLAIM_DEVICE_TYPE));
        if (userId == null || sessionId == null || deviceType == null) {
            throw unauthorized(ErrorCode.INVALID_REQUEST, "Invalid session token");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException(ErrorCode.DATA_NOT_FOUND, "User not found", HttpStatus.UNAUTHORIZED));

        UserAppSessionEntity session = userAppSessionRepository
                .findByUser_IdAndSessionIdAndDeviceType(userId, sessionId, deviceType)
                .orElse(null);

        if (session == null) {
            log.info("Session revoked for user {} token session {} device {}", userId, sessionId, deviceType);
            throw unauthorized(ErrorCode.SESSION_REVOKED, "Session has been replaced by another login");
        }

        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw unauthorized(ErrorCode.TOKEN_EXPIRED, "Session expired");
        }

        return userDetailsService.getUserById(userId);
    }

    private AppSessionDeviceType parseDeviceType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            return AppSessionDeviceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private InvalidRequestException unauthorized(String code, String message) {
        return new InvalidRequestException(code, message, HttpStatus.UNAUTHORIZED);
    }
}
