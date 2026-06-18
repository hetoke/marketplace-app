package com.marketplace.order.dto;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        String currency,
        String shippingAddress,
        Instant placedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        String cancelReason,
        boolean returnRequested,
        String returnReason,
        Instant returnRequestedAt,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId().toString(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getShippingAddress(),
                order.getPlacedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCancelReason(),
                order.isReturnRequested(),
                order.getReturnReason(),
                order.getReturnRequestedAt(),
                items,
                order.getCreatedAt()
        );
    }
}
