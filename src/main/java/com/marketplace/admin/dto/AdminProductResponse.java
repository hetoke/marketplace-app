package com.marketplace.admin.dto;

import com.marketplace.product.model.Product;
import java.time.Instant;
import java.util.UUID;

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
        return new AdminProductResponse(
            product.getId(),
            product.getName(),
            product.getSellerId(),
            product.getCategory() != null ? product.getCategory().getName() : null,
            product.getPrice(),
            product.getStock(),
            product.getAverageRating(),
            product.getReviewCount(),
            product.isActive(),
            product.getCreatedAt()
        );
    }
}
