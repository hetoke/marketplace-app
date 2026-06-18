package com.marketplace.order.dto;

import com.marketplace.order.model.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        String id,
        String productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId().toString(),
                item.getProductId().toString(),
                item.getProductName(),
                item.getProductImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }
}
