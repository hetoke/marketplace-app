package com.marketplace.product.dto;

import com.marketplace.product.model.ProductVariant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
        return new ProductVariantResponse(
                variant.getId().toString(),
                variant.getProduct().getId().toString(),
                variant.getSku(),
                variant.getPrice(),
                variant.getStock(),
                variant.getAttributes(),
                variant.isActive(),
                variant.getSortOrder(),
                variant.getCreatedAt()
        );
    }
}
