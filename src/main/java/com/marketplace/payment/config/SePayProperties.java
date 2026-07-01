package com.marketplace.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sepay")
public record SePayProperties(
    String merchantId,
    String secretKey,
    String webhookSecret,
    String environment,
    String successUrl,
    String errorUrl,
    String cancelUrl
) {
    public String getCheckoutUrl() {
        return "sandbox".equals(environment)
            ? "https://pay-sandbox.sepay.vn/v1/checkout/init"
            : "https://pay.sepay.vn/v1/checkout/init";
    }
}
