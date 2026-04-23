package com.nutalig.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtil {

    private static final SecretKey key = Keys.hmacShaKeyFor("k1tTjQwT4+rt4sFqF3Q9yP6azDmbzLb6zV9oS6dS5D0=".getBytes());
    private static final String ISSUER = "nutalig-api";

    public static String generateToken(String poId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(48 * 60 * 60); // 24hours

        return generateToken(poId, Map.of(), 48 * 60 * 60);
    }

    public static String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    public static String getClaim(String token, String claimName) {
        Object value = getClaims(token).get(claimName);
        return value == null ? null : String.valueOf(value);
    }

    public static boolean isValid(String token) {
        try {
            Claims claims = getClaims(token);
            return ISSUER.equals(claims.getIssuer()) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public static Boolean isTokenExpired(String token, String poId) {
        try {
            var claims = getClaims(token);
            // ✅ Check expired (จะ throw ExpiredJwtException ถ้าหมดอายุแล้ว)
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.info("Token expired at: " + expiration);
                return true;
            }

            // ✅ Check subject == poId
            String subject = claims.getSubject();
            if (!poId.equals(subject)) {
                log.warn("Token subject mismatch. Expected: " + poId + " but found: " + subject);
                return true;
            }

            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.info("Token expired at: " + e.getClaims().getExpiration());
            return true;
        } catch (Exception e) {
            System.out.println("Invalid token: " + e.getMessage());
            return true;
        }
    }

    private static Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String generateToken(String subject, Map<String, Object> claims, long expiresInSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiresInSeconds);

        Map<String, Object> allClaims = new HashMap<>(claims);

        return Jwts.builder()
                .claims(allClaims)
                .issuer(ISSUER)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

}
