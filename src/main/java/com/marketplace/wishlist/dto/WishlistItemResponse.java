package com.marketplace.wishlist.dto;

import com.marketplace.wishlist.model.WishlistItem;
import java.time.Instant;

public record WishlistItemResponse(
        String id,
        String productId,
        String productName,
        Instant addedAt
) {
    public static WishlistItemResponse from(WishlistItem item, String productName) {
        return new WishlistItemResponse(
                item.getId().toString(),
                item.getProductId().toString(),
                productName,
                item.getAddedAt()
        );
    }
}
