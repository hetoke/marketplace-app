package com.marketplace.admin.dto;

import com.marketplace.product.model.Product;
import java.time.Instant;
import java.util.UUID;

@lombok.Builder
public record AdminProductResponse(
    UUID id,
    String name,
    UUID sellerId,
    String categoryName,
    java.math.BigDecimal price,
    Integer stock,
    Double averageRating,
    Integer reviewCount,
    boolean active,
    Instant createdAt
) {
    public static AdminProductResponse from(Product product) {
        return AdminProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .sellerId(product.getSellerId())
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .price(product.getPrice())
            .stock(product.getStock())
            .averageRating(product.getAverageRating())
            .reviewCount(product.getReviewCount())
            .active(product.isActive())
            .createdAt(product.getCreatedAt())
            .build();
    }
}
