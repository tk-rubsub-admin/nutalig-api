package com.nutalig.service;

import com.nutalig.constant.ErrorCode;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRepository;
import com.nutalig.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppSessionService {

    private static final String TOKEN_TYPE_SESSION = "app_session";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_SESSION_ID = "sessionId";
    private static final long SESSION_EXPIRATION_SECONDS = 48 * 60 * 60;

    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public String issueSessionToken(UserEntity user) {
        String sessionId = UUID.randomUUID().toString();
        user.setCurrentSessionId(sessionId);
        userRepository.save(user);

        return JwtUtil.generateToken(
                user.getId(),
                Map.of(
                        CLAIM_TOKEN_TYPE, TOKEN_TYPE_SESSION,
                        CLAIM_SESSION_ID, sessionId
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
    }

    @Transactional
    public void revokeSessionByToken(String token) {
        if (StringUtils.isBlank(token) || !JwtUtil.isValid(token)) {
            return;
        }

        String tokenType = JwtUtil.getClaim(token, CLAIM_TOKEN_TYPE);
        String userId = StringUtils.trimToNull(JwtUtil.getSubject(token));
        String sessionId = StringUtils.trimToNull(JwtUtil.getClaim(token, CLAIM_SESSION_ID));

        if (!TOKEN_TYPE_SESSION.equals(tokenType) || userId == null || sessionId == null) {
            return;
        }

        userRepository.findById(userId).ifPresent(user -> {
            if (StringUtils.equals(sessionId, user.getCurrentSessionId())) {
                user.setCurrentSessionId(null);
                userRepository.save(user);
            }
        });
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
        if (userId == null || sessionId == null) {
            throw unauthorized(ErrorCode.INVALID_REQUEST, "Invalid session token");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException(ErrorCode.DATA_NOT_FOUND, "User not found", HttpStatus.UNAUTHORIZED));

        if (!StringUtils.equals(sessionId, user.getCurrentSessionId())) {
            log.info("Session revoked for user {} token session {} current session {}", userId, sessionId, user.getCurrentSessionId());
            throw unauthorized(ErrorCode.SESSION_REVOKED, "Session has been replaced by another login");
        }

        return userDetailsService.getUserById(userId);
    }

    private InvalidRequestException unauthorized(String code, String message) {
        return new InvalidRequestException(code, message, HttpStatus.UNAUTHORIZED);
    }
}
