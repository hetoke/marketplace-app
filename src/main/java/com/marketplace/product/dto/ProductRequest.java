package com.marketplace.product.dto;

import com.marketplace.product.model.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(min = 1, max = 255) String name,
        @Size(max = 5000) String description,
        @Positive BigDecimal price,
        @Min(0) Integer stock,
        DiscountType discountType,
        BigDecimal discountValue,
        Instant discountStart,
        Instant discountEnd,
        List<ProductVariantRequest> variants
) {}
