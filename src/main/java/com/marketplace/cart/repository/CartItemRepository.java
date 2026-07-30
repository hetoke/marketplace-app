package com.marketplace.cart.repository;

import com.marketplace.cart.model.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantId(UUID cartId, UUID productId, UUID variantId);

    void deleteByCartId(UUID cartId);

    int countByCartId(UUID cartId);
}
