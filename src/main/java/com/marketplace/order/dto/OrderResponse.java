package com.marketplace.order.dto;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@lombok.Builder
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
        int itemCount,
        Instant createdAt,
        String paymentId,
        BigDecimal discountAmount,
        BigDecimal originalAmount
) {
    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return from(order, items, null);
    }

    public static OrderResponse from(Order order, List<OrderItemResponse> items, String paymentId) {
        return OrderResponse.builder()
                .id(order.getId().toString())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .placedAt(order.getPlacedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .returnRequested(order.isReturnRequested())
                .returnReason(order.getReturnReason())
                .returnRequestedAt(order.getReturnRequestedAt())
                .items(items)
                .itemCount(items.stream().mapToInt(OrderItemResponse::quantity).sum())
                .createdAt(order.getCreatedAt())
                .paymentId(paymentId)
                .discountAmount(order.getDiscountAmount())
                .originalAmount(order.getOriginalAmount())
                .build();
    }
}
