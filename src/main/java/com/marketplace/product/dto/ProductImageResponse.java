package com.marketplace.product.dto;

import com.marketplace.product.model.ProductImage;

public record ProductImageResponse(
        String id,
        String url,
        String altText,
        Integer sortOrder,
        boolean isPrimary
) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId().toString(),
                image.getUrl(),
                image.getAltText(),
                image.getSortOrder(),
                image.isPrimary()
        );
    }
}
