package com.mailmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook")
public record WebhookProperties(
        int timeoutSeconds,
        int retryCount
) {
}
