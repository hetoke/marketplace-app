package com.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record InitiatePaymentRequest(
    @NotBlank String paymentMethod
) {}
