package com.mailmanager.security;

import com.mailmanager.config.AppSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final AppSecurityProperties securityProperties;

    public String issueToken(SessionTokenPayload payload, long ttlMinutes) {
        Instant now = Instant.now();
        SecretKey secretKey = Keys.hmacShaKeyFor(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(payload.actorId())
                .claim("actor_type", payload.actorType().name())
                .claim("scopes", payload.scopes())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public SessionPrincipal parse(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return new SessionPrincipal(
                Enum.valueOf(com.mailmanager.domain.ActorType.class, claims.get("actor_type", String.class)),
                claims.getSubject(),
                claims.get("scopes", java.util.List.class)
        );
    }
}
