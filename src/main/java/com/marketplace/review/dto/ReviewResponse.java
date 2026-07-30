package com.marketplace.review.dto;

import com.marketplace.review.model.Review;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID productId,
    UUID buyerId,
    String buyerName,
    Integer rating,
    String comment,
    boolean verifiedPurchase,
    UUID variantId,
    Map<String, String> variantAttributes,
    Instant createdAt,
    Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return from(review, null, null);
    }

    public static ReviewResponse from(Review review, String buyerName) {
        return from(review, buyerName, null);
    }

    public static ReviewResponse from(Review review, String buyerName, Map<String, String> variantAttributes) {
        return new ReviewResponse(
            review.getId(),
            review.getProductId(),
            review.getBuyerId(),
            buyerName,
            review.getRating(),
            review.getComment(),
            review.isVerifiedPurchase(),
            review.getVariantId(),
            variantAttributes,
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
