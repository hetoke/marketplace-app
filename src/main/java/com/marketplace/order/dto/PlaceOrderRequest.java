package com.marketplace.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @JsonProperty("shipping_address")
        @NotNull(message = "Shipping address is required")
        ShippingAddress shippingAddress
) {
    public record ShippingAddress(
            @NotBlank(message = "Street is required")
            String street,

            @NotBlank(message = "City is required")
            String city,

            @NotBlank(message = "State is required")
            String state,

            @NotBlank(message = "Zip is required")
            String zip,

            @NotBlank(message = "Country is required")
            String country
    ) {}
}
