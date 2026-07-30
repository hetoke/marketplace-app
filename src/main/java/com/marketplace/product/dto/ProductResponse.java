package com.marketplace.product.dto;

import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductImage;
import com.marketplace.product.model.ProductVariant;
import com.marketplace.product.model.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@lombok.Builder
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

        return ProductResponse.builder()
                .id(product.getId().toString())
                .sellerId(product.getSellerId().toString())
                .sellerName(sellerName)
                .category(CategoryResponse.from(product.getCategory()))
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .soldCount(product.getSoldCount())
                .isActive(product.isActive())
                .images(images.stream().map(ProductImageResponse::from).toList())
                .createdAt(product.getCreatedAt())
                .originalPrice(product.getPrice())
                .discountPrice(effectivePrice)
                .discountActive(discountActive)
                .discountType(product.getDiscountType())
                .discountValue(product.getDiscountValue())
                .timeLeft(timeLeft)
                .variants(variants)
                .build();
    }
}
