package com.marketplace.order.repository;

import com.marketplace.order.model.Order;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(UUID buyerId, com.marketplace.order.model.OrderStatus status);
}
