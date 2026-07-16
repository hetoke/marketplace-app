package com.marketplace.admin.controller;

import com.marketplace.admin.dto.OrderAnalyticsResponse;
import com.marketplace.admin.dto.ProductAnalyticsResponse;
import com.marketplace.admin.dto.RevenueAnalyticsResponse;
import com.marketplace.admin.dto.UserAnalyticsResponse;
import com.marketplace.admin.service.AdminAnalyticsService;
import com.marketplace.shared.dto.ApiResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RevenueAnalyticsResponse>> getRevenueAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Instant start = startDate != null
                ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate != null
                ? endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        RevenueAnalyticsResponse response = adminAnalyticsService.getRevenueAnalytics(start, end);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderAnalyticsResponse>> getOrderAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Instant start = startDate != null
                ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate != null
                ? endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        OrderAnalyticsResponse response = adminAnalyticsService.getOrderAnalytics(start, end);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Instant start = startDate != null
                ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate != null
                ? endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        UserAnalyticsResponse response = adminAnalyticsService.getUserAnalytics(start, end);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductAnalyticsResponse>> getProductAnalytics() {
        ProductAnalyticsResponse response = adminAnalyticsService.getProductAnalytics();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
