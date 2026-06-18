package com.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProcessPaymentRequest(
    @NotNull UUID orderId,
    @NotBlank String cardNumber,
    @NotBlank String cardHolder,
    @NotNull Integer expiryMonth,
    @NotNull Integer expiryYear,
    @NotBlank String cvv
) {}
