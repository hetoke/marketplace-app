package com.marketplace.webhook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook")
public record WebhookConfig(String secret) {}
