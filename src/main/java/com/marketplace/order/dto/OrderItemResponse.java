package com.marketplace.order.dto;

import com.marketplace.order.model.OrderItem;
import java.math.BigDecimal;

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
        return new OrderItemResponse(
                item.getId().toString(),
                item.getProductId().toString(),
                item.getVariantId() != null ? item.getVariantId().toString() : null,
                item.getSku(),
                item.getProductName(),
                item.getProductImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice(),
                item.getDiscountAmount()
        );
    }
}
