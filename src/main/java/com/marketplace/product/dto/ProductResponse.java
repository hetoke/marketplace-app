package com.marketplace.product.dto;

import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductImage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        String id,
        String sellerId,
        CategoryResponse category,
        String name,
        String slug,
        String description,
        BigDecimal price,
        Integer stock,
        Double averageRating,
        Integer reviewCount,
        boolean isActive,
        List<ProductImageResponse> images,
        Instant createdAt,
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        boolean discountActive
) {
    public static ProductResponse from(Product product, List<ProductImage> images) {
        return new ProductResponse(
                product.getId().toString(),
                product.getSellerId().toString(),
                CategoryResponse.from(product.getCategory()),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.isActive(),
                images.stream().map(ProductImageResponse::from).toList(),
                product.getCreatedAt(),
                product.getPrice(),
                product.getPrice(),
                false
        );
    }

    public static ProductResponse from(Product product, List<ProductImage> images, BigDecimal effectivePrice, boolean discountActive) {
        return new ProductResponse(
                product.getId().toString(),
                product.getSellerId().toString(),
                CategoryResponse.from(product.getCategory()),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.isActive(),
                images.stream().map(ProductImageResponse::from).toList(),
                product.getCreatedAt(),
                product.getPrice(),
                effectivePrice,
                discountActive
        );
    }

    public static ProductResponse from(Product product) {
        return from(product, List.of());
    }
}
