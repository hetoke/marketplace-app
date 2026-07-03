package com.marketplace.cart.service;

import com.marketplace.cart.dto.CartItemResponse;
import com.marketplace.cart.dto.CartResponse;
import com.marketplace.cart.model.Cart;
import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final int MAX_RETRIES = 3;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String userId) {
        UUID userUuid = UUID.fromString(userId);
        Cart cart = getOrCreateActiveCart(userUuid);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String userId, UUID productId, int quantity) {
        UUID userUuid = UUID.fromString(userId);
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return doAddItem(userUuid, productId, quantity);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock on addItem (attempt {}/{}), retrying", attempt + 1, MAX_RETRIES);
                entityManager.clear();
            }
        }
        throw new BusinessException("Cart is being updated by another request. Please try again.");
    }

    private CartResponse doAddItem(UUID userUuid, UUID productId, int quantity) {
        Cart cart = getOrCreateActiveCart(userUuid);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!product.isActive()) {
            throw new BusinessException("Product is not available");
        }

        if (product.getStock() < quantity) {
            throw new BusinessException("Insufficient stock. Available: " + product.getStock());
        }

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            if (product.getStock() < newQuantity) {
                throw new BusinessException("Insufficient stock. Available: " + product.getStock());
            }
            int delta = newQuantity - existingItem.getQuantity();
            int updated = productRepository.decrementStock(productId, delta);
            if (updated == 0) {
                throw new BusinessException("Insufficient stock. Available: " + productRepository.findById(productId).map(Product::getStock).orElse(0));
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(product.getPrice());
            cartItemRepository.save(existingItem);
        } else {
            int updated = productRepository.decrementStock(productId, quantity);
            if (updated == 0) {
                throw new BusinessException("Insufficient stock. Available: " + productRepository.findById(productId).map(Product::getStock).orElse(0));
            }
            CartItem newItem = new CartItem(cart, productId, quantity, product.getPrice());
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(String userId, UUID itemId, int quantity) {
        UUID userUuid = UUID.fromString(userId);
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return doUpdateQuantity(userUuid, itemId, quantity);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock on updateQuantity (attempt {}/{}), retrying", attempt + 1, MAX_RETRIES);
                entityManager.clear();
            }
        }
        throw new BusinessException("Cart is being updated by another request. Please try again.");
    }

    private CartResponse doUpdateQuantity(UUID userUuid, UUID itemId, int quantity) {
        Cart cart = getOrCreateActiveCart(userUuid);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AccessDeniedException("Cart item does not belong to this cart");
        }

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

        int delta = quantity - item.getQuantity();
        if (delta > 0 && product.getStock() < delta) {
            throw new BusinessException("Insufficient stock. Available: " + product.getStock());
        }

        if (delta > 0) {
            int updated = productRepository.decrementStock(item.getProductId(), delta);
            if (updated == 0) {
                throw new BusinessException("Insufficient stock. Available: " + productRepository.findById(item.getProductId()).map(Product::getStock).orElse(0));
            }
        } else if (delta < 0) {
            productRepository.incrementStock(item.getProductId(), -delta);
        }

        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String userId, UUID itemId) {
        UUID userUuid = UUID.fromString(userId);
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return doRemoveItem(userUuid, itemId);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock on removeItem (attempt {}/{}), retrying", attempt + 1, MAX_RETRIES);
                entityManager.clear();
            }
        }
        throw new BusinessException("Cart is being updated by another request. Please try again.");
    }

    private CartResponse doRemoveItem(UUID userUuid, UUID itemId) {
        Cart cart = getOrCreateActiveCart(userUuid);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AccessDeniedException("Cart item does not belong to this cart");
        }

        productRepository.incrementStock(item.getProductId(), item.getQuantity());
        cartItemRepository.delete(item);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(String userId) {
        UUID userUuid = UUID.fromString(userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE)
                .orElse(null);
        if (cart == null) {
            return;
        }
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        for (CartItem item : items) {
            productRepository.incrementStock(item.getProductId(), item.getQuantity());
        }
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(String userId) {
        UUID userUuid = UUID.fromString(userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE)
                .orElse(null);
        if (cart == null) {
            return List.of();
        }
        return cartItemRepository.findByCartId(cart.getId());
    }

    private Cart getOrCreateActiveCart(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                .orElseGet(() -> {
                    Cart cart = new Cart(userId);
                    return cartRepository.save(cart);
                });
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    String productName = product != null ? product.getName() : "Unknown Product";
                    int stock = product != null ? product.getStock() : 0;
                    return CartItemResponse.from(item, productName, stock);
                })
                .toList();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId().toString(),
                itemResponses,
                totalAmount,
                items.size()
        );
    }
}
