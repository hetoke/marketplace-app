package com.marketplace.payment.repository;

import com.marketplace.payment.model.Payment;
import com.marketplace.payment.model.Payment.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    List<Payment> findByOrderId(UUID orderId);

    @Query("SELECT p FROM Payment p WHERE p.orderId IN " +
           "(SELECT o.id FROM Order o WHERE o.buyerId = :buyerId) ORDER BY p.createdAt DESC")
    List<Payment> findByBuyerId(@Param("buyerId") UUID buyerId);

    @Query(value = "SELECT p.* FROM payments p " +
           "JOIN orders o ON p.order_id = o.id " +
           "JOIN order_items oi ON o.id = oi.order_id " +
           "JOIN products pr ON oi.product_id = pr.id " +
           "WHERE pr.seller_id = :sellerId AND p.status = 'COMPLETED' " +
           "ORDER BY p.created_at DESC", nativeQuery = true)
    List<Payment> findCompletedBySellerId(@Param("sellerId") UUID sellerId);
}
