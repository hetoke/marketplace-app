package com.marketplace.order.dto;

import com.marketplace.order.model.OrderItem;
import java.math.BigDecimal;

@lombok.Builder
public record OrderItemResponse(
        String id,
        String productId,
        String variantId,
        String sku,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice,
        BigDecimal discountAmount
) {
    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId().toString())
                .productId(item.getProductId().toString())
                .variantId(item.getVariantId() != null ? item.getVariantId().toString() : null)
                .sku(item.getSku())
                .productName(item.getProductName())
                .productImageUrl(item.getProductImageUrl())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .discountAmount(item.getDiscountAmount())
                .build();
    }
}
