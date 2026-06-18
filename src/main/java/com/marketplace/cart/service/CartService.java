package com.marketplace.cart.service;

import com.marketplace.cart.dto.CartItemResponse;
import com.marketplace.cart.dto.CartResponse;
import com.marketplace.cart.model.Cart;
import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

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
            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(product.getPrice());
            cartItemRepository.save(existingItem);
        } else {
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
        Cart cart = getOrCreateActiveCart(userUuid);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BusinessException("Cart item does not belong to this cart");
        }

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

        if (product.getStock() < quantity) {
            throw new BusinessException("Insufficient stock. Available: " + product.getStock());
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
        Cart cart = getOrCreateActiveCart(userUuid);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BusinessException("Cart item does not belong to this cart");
        }

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
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
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
                    String productName = productRepository.findById(item.getProductId())
                            .map(Product::getName)
                            .orElse("Unknown Product");
                    return CartItemResponse.from(item, productName);
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
