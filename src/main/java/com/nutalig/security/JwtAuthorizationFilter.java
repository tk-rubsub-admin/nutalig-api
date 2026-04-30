package com.nutalig.security;

import com.nutalig.constant.ErrorCode;
import com.nutalig.constant.ResponseStatus;
import com.nutalig.dto.UserDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.AppSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final AppSessionService appSessionService;
    private final AuthErrorResponseWriter authErrorResponseWriter;

    private static final List<String> WHITELIST = List.of(
            "/v1/login",
            "/v1/auth/line/login",
            "/v1/auth/line/register",
            "/v1/auth/line/login/url",
            "/v1/auth/line/register/validate",
            "/v1/auth/line/register/url",
            "/v1/auth/line/callback",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/line-webhook",
            "/uploads"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = normalizePath(request);
        boolean skip = WHITELIST.stream().anyMatch(path::startsWith);

        log.info("[JWTFilter] path: {} skip: {}", path, skip );
        return skip;
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return path.isBlank() ? "/" : path;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Accept, X-Requested-With, remember-me, Authorization");

        // Handle CORS pre-flight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            authErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.INVALID_REQUEST, "Missing bearer token");
            return;
        }

        String token = authHeader.substring(7);

        try {
            UserDto userDto = appSessionService.authenticate(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDto,
                            null,
                            userDto.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (DataNotFoundException | InvalidRequestException e) {
            log.warn("App session authentication failed: {}", e.getMessage());
            authErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    resolveErrorCode(e), e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveErrorCode(Exception exception) {
        if (exception instanceof InvalidRequestException invalidRequestException
                && invalidRequestException.getCode() != null) {
            return invalidRequestException.getCode();
        }
        if (exception instanceof DataNotFoundException dataNotFoundException
                && dataNotFoundException.getCode() != null) {
            return dataNotFoundException.getCode();
        }
        return ResponseStatus.FAILED;
    }
}
