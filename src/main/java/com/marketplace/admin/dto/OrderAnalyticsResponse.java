package com.marketplace.admin.dto;

import java.util.List;
import java.util.Map;

public record OrderAnalyticsResponse(
    long totalOrders,
    Map<String, Long> ordersByStatus,
    double cancellationRate,
    List<DailyOrders> dailyBreakdown
) {
    public record DailyOrders(String date, long orderCount, long revenue) {}
}
