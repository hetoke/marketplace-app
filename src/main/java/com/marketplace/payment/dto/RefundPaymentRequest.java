package com.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundPaymentRequest(
    @NotBlank String reason
) {}
