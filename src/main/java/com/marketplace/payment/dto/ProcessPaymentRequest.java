package com.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
    @NotBlank String cardNumber,
    @NotBlank String cardHolder,
    @NotNull Integer expiryMonth,
    @NotNull Integer expiryYear,
    @NotBlank String cvv
) {}
