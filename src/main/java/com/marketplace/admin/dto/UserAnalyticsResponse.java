package com.marketplace.admin.dto;

import java.util.List;
import java.util.Map;

public record UserAnalyticsResponse(
    long totalUsers,
    Map<String, Long> usersByRole,
    long verifiedUsers,
    List<DailyUsers> dailyBreakdown
) {
    public record DailyUsers(String date, long newUsers) {}
}
