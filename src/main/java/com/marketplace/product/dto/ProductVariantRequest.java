package com.marketplace.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantRequest(
        @NotBlank String sku,
        @NotNull @Positive BigDecimal price,
        @NotNull @Min(0) Integer stock,
        Map<String, String> attributes,
        Integer sortOrder
) {}
