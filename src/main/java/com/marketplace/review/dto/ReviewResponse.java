package com.marketplace.review.dto;

import com.marketplace.review.model.Review;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID productId,
    UUID buyerId,
    String buyerName,
    Integer rating,
    String comment,
    boolean verifiedPurchase,
    Instant createdAt,
    Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getProductId(),
            review.getBuyerId(),
            null,
            review.getRating(),
            review.getComment(),
            review.isVerifiedPurchase(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }

    public static ReviewResponse from(Review review, String buyerName) {
        return new ReviewResponse(
            review.getId(),
            review.getProductId(),
            review.getBuyerId(),
            buyerName,
            review.getRating(),
            review.getComment(),
            review.isVerifiedPurchase(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
