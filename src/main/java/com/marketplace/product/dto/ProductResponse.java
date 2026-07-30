package com.marketplace.product.dto;

import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductImage;
import com.marketplace.product.model.ProductVariant;
import com.marketplace.product.model.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        String id,
        String sellerId,
        String sellerName,
        CategoryResponse category,
        String name,
        String slug,
        String description,
        BigDecimal price,
        Integer stock,
        Double averageRating,
        Integer reviewCount,
        Integer soldCount,
        boolean isActive,
        List<ProductImageResponse> images,
        Instant createdAt,
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        boolean discountActive,
        DiscountType discountType,
        BigDecimal discountValue,
        Long timeLeft,
        List<ProductVariantResponse> variants
) {
    public static ProductResponse from(Product product, List<ProductImage> images) {
        return buildResponse(product, images, product.getPrice(), false, null, null);
    }

    public static ProductResponse from(Product product, List<ProductImage> images, BigDecimal effectivePrice, boolean discountActive) {
        return buildResponse(product, images, effectivePrice, discountActive, null, null);
    }

    public static ProductResponse from(Product product, List<ProductImage> images, BigDecimal effectivePrice, boolean discountActive, String sellerName) {
        return buildResponse(product, images, effectivePrice, discountActive, sellerName, null);
    }

    public static ProductResponse from(Product product, List<ProductImage> images, BigDecimal effectivePrice, boolean discountActive, String sellerName, Long timeLeft) {
        return buildResponse(product, images, effectivePrice, discountActive, sellerName, timeLeft);
    }

    private static ProductResponse buildResponse(Product product, List<ProductImage> images, BigDecimal effectivePrice, boolean discountActive, String sellerName, Long timeLeft) {
        List<ProductVariantResponse> variants = product.getVariants() != null
                ? product.getVariants().stream().map(ProductVariantResponse::from).toList()
                : List.of();

        return new ProductResponse(
                product.getId().toString(),
                product.getSellerId().toString(),
                sellerName,
                CategoryResponse.from(product.getCategory()),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getSoldCount(),
                product.isActive(),
                images.stream().map(ProductImageResponse::from).toList(),
                product.getCreatedAt(),
                product.getPrice(),
                effectivePrice,
                discountActive,
                product.getDiscountType(),
                product.getDiscountValue(),
                timeLeft,
                variants
        );
    }
}
