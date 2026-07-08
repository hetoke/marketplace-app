package com.marketplace.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueAnalyticsResponse(
    BigDecimal totalRevenue,
    BigDecimal averageOrderValue,
    long totalOrders,
    List<DailyRevenue> dailyBreakdown
) {
    public record DailyRevenue(String date, BigDecimal revenue, long orderCount) {}
}
