package com.marketplace.payment.dto;

import com.marketplace.payment.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentHistoryResponse(
    String id,
    String orderId,
    BigDecimal amount,
    String currency,
    String method,
    String status,
    Instant createdAt
) {
    public static PaymentHistoryResponse from(Payment payment) {
        return new PaymentHistoryResponse(
            payment.getId().toString(),
            payment.getOrderId().toString(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getMethod().name(),
            payment.getStatus().name(),
            payment.getCreatedAt()
        );
    }
}
