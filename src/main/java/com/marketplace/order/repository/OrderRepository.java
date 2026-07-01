package com.marketplace.order.repository;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.Order.PaymentStatus;
import com.marketplace.order.model.OrderStatus;
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
}
