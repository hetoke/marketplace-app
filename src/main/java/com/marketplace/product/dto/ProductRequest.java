package com.marketplace.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(min = 1, max = 255) String name,
        @Size(max = 5000) String description,
        @NotNull @Positive BigDecimal price,
        @NotNull @Min(0) Integer stock
) {}
