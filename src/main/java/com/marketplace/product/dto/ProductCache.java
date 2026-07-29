package com.marketplace.product.dto;

import java.util.List;

public record ProductCache(
        List<ProductResponse> items
) {}
