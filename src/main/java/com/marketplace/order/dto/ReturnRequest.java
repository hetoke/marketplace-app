package com.marketplace.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnRequest(
        @NotBlank(message = "Return reason is required")
        String reason
) {}
