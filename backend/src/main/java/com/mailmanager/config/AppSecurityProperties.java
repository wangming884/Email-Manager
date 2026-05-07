package com.mailmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        String adminUsername,
        String adminPassword,
        String encryptionSecret,
        String jwtSecret,
        long adminTokenTtlMinutes,
        long clientTokenTtlMinutes
) {
}
