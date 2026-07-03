package com.marketplace.cart.service;

import com.marketplace.cart.model.Cart;
import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.product.repository.ProductRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartExpirationService {

    private static final Logger log = LoggerFactory.getLogger(CartExpirationService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartExpirationService(CartRepository cartRepository,
                                  CartItemRepository cartItemRepository,
                                  ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireAbandonedCarts() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant now = Instant.now();

        List<Cart> expiredCarts = cartRepository.findExpiredCarts(Cart.Status.ACTIVE, cutoff);

        for (Cart cart : expiredCarts) {
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
            for (CartItem item : items) {
                productRepository.incrementStock(item.getProductId(), item.getQuantity());
            }
            cart.setStatus(Cart.Status.ABANDONED);
            cart.setUpdatedAt(now);
            cartRepository.save(cart);
        }

        if (!expiredCarts.isEmpty()) {
            log.info("Expired {} abandoned cart(s) older than 1 day", expiredCarts.size());
        }
    }
}
