package com.marketplace.cart.dto;

import com.marketplace.cart.model.CartItem;
import java.math.BigDecimal;
import java.util.UUID;

@lombok.Builder
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
        return CartItemResponse.builder()
                .id(item.getId().toString())
                .productId(item.getProductId().toString())
                .variantId(item.getVariantId() != null ? item.getVariantId().toString() : null)
                .sku(item.getSku())
                .productName(productName)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .stock(stock)
                .discountAmount(item.getDiscountAmount())
                .build();
    }
}
