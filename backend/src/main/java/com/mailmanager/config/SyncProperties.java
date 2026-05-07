package com.mailmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sync")
public record SyncProperties(
        int providerConcurrency,
        int jitterMinMillis,
        int jitterMaxMillis,
        int fetchLimit
) {
}
