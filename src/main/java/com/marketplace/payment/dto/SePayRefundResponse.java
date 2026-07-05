package com.marketplace.payment.dto;

public record SePayRefundResponse(
    boolean success,
    String message,
    String orderInvoiceNumber
) {
    public static SePayRefundResponse success(String orderInvoiceNumber) {
        return new SePayRefundResponse(true, "Refund processed successfully", orderInvoiceNumber);
    }

    public static SePayRefundResponse failure(String message) {
        return new SePayRefundResponse(false, message, null);
    }
}
