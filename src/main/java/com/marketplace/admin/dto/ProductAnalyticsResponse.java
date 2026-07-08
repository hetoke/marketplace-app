package com.marketplace.admin.dto;

import java.util.List;
import java.util.Map;

public record ProductAnalyticsResponse(
    long totalProducts,
    long activeProducts,
    long inactiveProducts,
    long lowStockProducts,
    Map<String, Long> productsByCategory,
    List<TopProduct> topRatedProducts
) {
    public record TopProduct(String name, double averageRating, long reviewCount) {}
}
