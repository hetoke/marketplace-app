package com.marketplace.cart.repository;

import com.marketplace.cart.model.Cart;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, Cart.Status status);

    Optional<Cart> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Cart> findByStatusAndUpdatedAtBefore(Cart.Status status, Instant updatedAt);

    @Modifying
    @Query("UPDATE Cart c SET c.status = :newStatus, c.updatedAt = :now WHERE c.status = :currentStatus AND c.updatedAt < :cutoff")
    int updateStatusByUpdatedAtBefore(@Param("currentStatus") Cart.Status currentStatus,
                                       @Param("newStatus") Cart.Status newStatus,
                                       @Param("cutoff") Instant cutoff,
                                       @Param("now") Instant now);

    @Query("SELECT c FROM Cart c WHERE c.status = :status AND c.updatedAt < :cutoff")
    List<Cart> findExpiredCarts(@Param("status") Cart.Status status, @Param("cutoff") Instant cutoff);
}
