package com.marketplace.payment.dto;

import com.marketplace.payment.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;

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
        return new PaymentResponse(
            payment.getId().toString(),
            payment.getOrderId().toString(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getMethod().name(),
            payment.getStatus().name(),
            payment.getCardLastFour(),
            payment.getCardBrand(),
            payment.getProviderRef(),
            payment.getInvoiceNumber(),
            payment.getFailureReason(),
            payment.getRefundedAt(),
            payment.getCreatedAt()
        );
    }
}
