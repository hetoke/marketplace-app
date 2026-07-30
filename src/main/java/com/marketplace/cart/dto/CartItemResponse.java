package com.marketplace.cart.dto;

import com.marketplace.cart.model.CartItem;
import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        String id,
        String productId,
        String variantId,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Integer stock,
        BigDecimal discountAmount
) {
    public static CartItemResponse from(CartItem item, String productName, int stock) {
        return new CartItemResponse(
                item.getId().toString(),
                item.getProductId().toString(),
                item.getVariantId() != null ? item.getVariantId().toString() : null,
                item.getSku(),
                productName,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                stock,
                item.getDiscountAmount()
        );
    }
}
