package com.marketplace.product.dto;

import java.util.List;

public record CategoryCache(
        List<CategoryResponse> items
) {}
