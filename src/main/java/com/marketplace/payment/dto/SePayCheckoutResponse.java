package com.marketplace.payment.dto;

import java.util.Map;

public record SePayCheckoutResponse(
    String checkoutUrl,
    Map<String, String> formFields
) {}
