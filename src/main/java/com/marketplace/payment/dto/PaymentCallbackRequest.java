package com.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record PaymentCallbackRequest(
    @NotBlank String orderId,
    @NotBlank String paymentStatus,
    Map<String, String> params
) {}
