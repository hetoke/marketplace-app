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

            @NotBlank(message = "Ward/Commune is required")
            String ward,

            @NotBlank(message = "District is required")
            String district,

            @NotBlank(message = "Province is required")
            String province,

            @NotBlank(message = "Country is required")
            String country
    ) {}
}
