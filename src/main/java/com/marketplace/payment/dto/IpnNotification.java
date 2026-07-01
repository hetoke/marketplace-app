package com.marketplace.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IpnNotification(
    long timestamp,
    @JsonProperty("notification_type") String notificationType,
    OrderInfo order,
    TransactionInfo transaction,
    CustomerInfo customer,
    Object agreement
) {
    public record OrderInfo(
        String id,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("order_status") String orderStatus,
        @JsonProperty("order_currency") String orderCurrency,
        @JsonProperty("order_amount") String orderAmount,
        @JsonProperty("order_invoice_number") String orderInvoiceNumber,
        @JsonProperty("custom_data") List<Object> customData,
        @JsonProperty("user_agent") String userAgent,
        @JsonProperty("ip_address") String ipAddress,
        @JsonProperty("order_description") String orderDescription
    ) {}

    public record TransactionInfo(
        String id,
        @JsonProperty("payment_method") String paymentMethod,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("transaction_type") String transactionType,
        @JsonProperty("transaction_date") String transactionDate,
        @JsonProperty("transaction_status") String transactionStatus,
        @JsonProperty("transaction_amount") String transactionAmount,
        @JsonProperty("transaction_currency") String transactionCurrency,
        @JsonProperty("authentication_status") String authenticationStatus,
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("card_holder_name") String cardHolderName,
        @JsonProperty("card_expiry") String cardExpiry,
        @JsonProperty("card_funding_method") String cardFundingMethod,
        @JsonProperty("card_brand") String cardBrand
    ) {}

    public record CustomerInfo(
        String id,
        @JsonProperty("customer_id") String customerId
    ) {}
}
