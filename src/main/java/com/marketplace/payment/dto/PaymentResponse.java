package com.marketplace.payment.dto;

import com.marketplace.payment.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;

@lombok.Builder
public record PaymentResponse(
    String id,
    String orderId,
    BigDecimal amount,
    String currency,
    String method,
    String status,
    String cardLastFour,
    String cardBrand,
    String providerRef,
    String invoiceNumber,
    String failureReason,
    Instant refundedAt,
    Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId().toString())
            .orderId(payment.getOrderId().toString())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .method(payment.getMethod().name())
            .status(payment.getStatus().name())
            .cardLastFour(payment.getCardLastFour())
            .cardBrand(payment.getCardBrand())
            .providerRef(payment.getProviderRef())
            .invoiceNumber(payment.getInvoiceNumber())
            .failureReason(payment.getFailureReason())
            .refundedAt(payment.getRefundedAt())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
