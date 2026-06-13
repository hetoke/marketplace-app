package com.marketplace.product.dto;

import com.marketplace.product.model.Category;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        String id,
        String name,
        String description,
        String slug,
        String parentId,
        boolean isActive,
        Instant createdAt
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId().toString(),
                category.getName(),
                category.getDescription(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId().toString() : null,
                category.isActive(),
                category.getCreatedAt()
        );
    }
}
