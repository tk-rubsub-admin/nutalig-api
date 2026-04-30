package com.nutalig.security;

import com.nutalig.constant.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthErrorResponseWriter authErrorResponseWriter;
    private final JwtAuthorizationFilter tokenAuthenticationFilter;
    private final SecurityContextRepository securityContextRepository;

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, e) -> authErrorResponseWriter.write(
                request,
                response,
                401,
                ErrorCode.INVALID_REQUEST,
                "Unauthorized"
        );
    }

    // Configuring HttpSecurity
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v1/login").permitAll()
                        .requestMatchers("/v1/auth/line/login").permitAll()
                        .requestMatchers("/v1/auth/line/register").permitAll()
                        .requestMatchers("/v1/auth/line/login/url").permitAll()
                        .requestMatchers("/v1/auth/line/register/validate").permitAll()
                        .requestMatchers("/v1/auth/line/register/url").permitAll()
                        .requestMatchers("/v1/auth/line/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/line-webhook").permitAll()
                .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            restAuthenticationEntryPoint().commence(req, res, ex);
                        }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(securityContextCustomizer -> securityContextCustomizer.securityContextRepository(securityContextRepository))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        //// @formatter:on

        return http.build();
    }
}
