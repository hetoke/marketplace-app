package com.marketplace.admin.service;

import com.marketplace.admin.dto.OrderAnalyticsResponse;
import com.marketplace.admin.dto.ProductAnalyticsResponse;
import com.marketplace.admin.dto.RevenueAnalyticsResponse;
import com.marketplace.admin.dto.UserAnalyticsResponse;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.payment.model.Payment.PaymentStatus;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.review.repository.ReviewRepository;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    public AdminAnalyticsService(UserRepository userRepository,
                                  ProductRepository productRepository,
                                  OrderRepository orderRepository,
                                  PaymentRepository paymentRepository,
                                  ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analyticsRevenue", key = "'revenue:' + #startDate + ':' + #endDate")
    public RevenueAnalyticsResponse getRevenueAnalytics(Instant startDate, Instant endDate) {
        BigDecimal totalRevenue = paymentRepository.sumAmountByStatusAndCreatedAtBetween(
                PaymentStatus.COMPLETED, startDate, endDate);

        long totalOrders = orderRepository.countByCreatedAtBetween(startDate, endDate);

        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> dailyData = orderRepository.countByCreatedAtBetweenGroupByDate(startDate, endDate);
        List<RevenueAnalyticsResponse.DailyRevenue> dailyBreakdown = new ArrayList<>();
        for (Object[] row : dailyData) {
            String date = row[0] != null ? row[0].toString() : "unknown";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
            BigDecimal revenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            dailyBreakdown.add(new RevenueAnalyticsResponse.DailyRevenue(date, revenue, count));
        }

        return new RevenueAnalyticsResponse(totalRevenue, avgOrderValue, totalOrders, dailyBreakdown);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analyticsOrders", key = "'orders:' + #startDate + ':' + #endDate")
    public OrderAnalyticsResponse getOrderAnalytics(Instant startDate, Instant endDate) {
        long totalOrders = orderRepository.countByCreatedAtBetween(startDate, endDate);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            ordersByStatus.put(status.name(), count);
        }

        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        double cancellationRate = totalOrders > 0
                ? (double) cancelledOrders / totalOrders * 100.0
                : 0.0;

        List<Object[]> dailyData = orderRepository.countByCreatedAtBetweenGroupByDate(startDate, endDate);
        List<OrderAnalyticsResponse.DailyOrders> dailyBreakdown = new ArrayList<>();
        for (Object[] row : dailyData) {
            String date = row[0] != null ? row[0].toString() : "unknown";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
            long revenue = row[2] != null ? ((Number) row[2]).longValue() : 0;
            dailyBreakdown.add(new OrderAnalyticsResponse.DailyOrders(date, count, revenue));
        }

        return new OrderAnalyticsResponse(totalOrders, ordersByStatus, cancellationRate, dailyBreakdown);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analyticsUsers", key = "'users:' + #startDate + ':' + #endDate")
    public UserAnalyticsResponse getUserAnalytics(Instant startDate, Instant endDate) {
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.countByVerifiedTrue();

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (User.Role role : User.Role.values()) {
            long count = userRepository.countByRole(role);
            usersByRole.put(role.name(), count);
        }

        List<Object[]> dailyData = userRepository.countByCreatedAtBetweenGroupByDate(startDate, endDate);
        List<UserAnalyticsResponse.DailyUsers> dailyBreakdown = new ArrayList<>();
        for (Object[] row : dailyData) {
            String date = row[0] != null ? row[0].toString() : "unknown";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
            dailyBreakdown.add(new UserAnalyticsResponse.DailyUsers(date, count));
        }

        return new UserAnalyticsResponse(totalUsers, usersByRole, verifiedUsers, dailyBreakdown);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analyticsProducts", key = "'products'")
    public ProductAnalyticsResponse getProductAnalytics() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByActiveTrue();
        long inactiveProducts = productRepository.countByActiveFalse();

        List<Product> lowStockProducts = productRepository.findByStockLessThan(5);
        long lowStockCount = lowStockProducts.size();

        Map<String, Long> productsByCategory = new LinkedHashMap<>();
        List<com.marketplace.product.model.Category> categories = new ArrayList<>();
        productRepository.findAll().forEach(p -> {
            if (p.getCategory() != null) {
                String catName = p.getCategory().getName();
                productsByCategory.merge(catName, 1L, Long::sum);
            }
        });

        List<Product> topRated = productRepository.findAll().stream()
                .filter(p -> p.getReviewCount() > 0)
                .sorted((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
                .limit(5)
                .toList();

        List<ProductAnalyticsResponse.TopProduct> topRatedProducts = topRated.stream()
                .map(p -> new ProductAnalyticsResponse.TopProduct(p.getName(), p.getAverageRating(), p.getReviewCount()))
                .toList();

        return new ProductAnalyticsResponse(
                totalProducts, activeProducts, inactiveProducts, lowStockCount,
                productsByCategory, topRatedProducts);
    }
}
