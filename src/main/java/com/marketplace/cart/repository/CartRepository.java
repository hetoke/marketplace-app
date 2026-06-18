package com.marketplace.cart.repository;

import com.marketplace.cart.model.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, Cart.Status status);

    Optional<Cart> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
