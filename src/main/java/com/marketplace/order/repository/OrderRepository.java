package com.marketplace.order.repository;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.Order.PaymentStatus;
import com.marketplace.order.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(UUID buyerId, com.marketplace.order.model.OrderStatus status);

    @Modifying
    @Query("UPDATE Order o SET o.paymentStatus = :status, o.updatedAt = :updatedAt WHERE o.id = :orderId")
    int updatePaymentStatus(@Param("orderId") UUID orderId, @Param("status") PaymentStatus status, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("UPDATE Order o SET o.paymentStatus = :paymentStatus, o.status = :orderStatus, o.updatedAt = :updatedAt WHERE o.id = :orderId AND o.status = com.marketplace.order.model.OrderStatus.PENDING")
    int confirmOnPayment(@Param("orderId") UUID orderId, @Param("paymentStatus") PaymentStatus paymentStatus, @Param("orderStatus") OrderStatus orderStatus, @Param("updatedAt") Instant updatedAt);

    long countByStatus(OrderStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.paymentStatus = com.marketplace.order.model.Order.PaymentStatus.PAID")
    BigDecimal sumTotalAmountByCreatedAtBetweenAndPaid(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT FUNCTION('DATE', o.createdAt) as date, COUNT(o) as cnt, COALESCE(SUM(o.totalAmount), 0) as revenue FROM Order o " +
           "WHERE o.createdAt BETWEEN :start AND :end GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY date")
    List<Object[]> countByCreatedAtBetweenGroupByDate(
            @Param("start") Instant start, @Param("end") Instant end);
}
