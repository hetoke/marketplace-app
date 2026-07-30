package com.marketplace.product.dto;

import com.marketplace.product.model.ProductVariant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@lombok.Builder
public record ProductVariantResponse(
        String id,
        String productId,
        String sku,
        BigDecimal price,
        Integer stock,
        Map<String, String> attributes,
        boolean isActive,
        Integer sortOrder,
        Instant createdAt
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId().toString())
                .productId(variant.getProduct().getId().toString())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .stock(variant.getStock())
                .attributes(variant.getAttributes())
                .isActive(variant.isActive())
                .sortOrder(variant.getSortOrder())
                .createdAt(variant.getCreatedAt())
                .build();
    }
}
