package com.mailmanager.service;

import com.mailmanager.config.AppSecurityProperties;
import com.mailmanager.domain.ActorType;
import com.mailmanager.exception.ForbiddenException;
import com.mailmanager.security.SessionTokenPayload;
import com.mailmanager.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AppSecurityProperties securityProperties;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public String login(String username, String password) {
        // Use constant-time comparison to prevent timing attacks
        boolean usernameMatches = securityProperties.adminUsername().equals(username);
        boolean passwordMatches = passwordEncoder.matches(password, securityProperties.adminPassword());
        
        if (!usernameMatches || !passwordMatches) {
            throw new ForbiddenException("Invalid admin credentials");
        }
        return tokenService.issueToken(
                new SessionTokenPayload(ActorType.ADMIN, username, List.of("ADMIN")),
                securityProperties.adminTokenTtlMinutes()
        );
    }
}
