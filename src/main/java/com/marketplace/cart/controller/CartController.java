package com.marketplace.cart.controller;

import com.marketplace.cart.dto.AddToCartRequest;
import com.marketplace.cart.dto.CartResponse;
import com.marketplace.cart.dto.UpdateCartItemRequest;
import com.marketplace.cart.service.CartService;
import com.marketplace.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cart = cartService.addItem(userId, request.productId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cart = cartService.updateQuantity(userId, itemId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok("Cart item updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable UUID itemId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cart = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
