package com.marketplace.wishlist.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.wishlist.dto.WishlistItemResponse;
import com.marketplace.wishlist.service.WishlistService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buyers/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<WishlistItemResponse> items = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(
            @PathVariable UUID productId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        WishlistItemResponse item = wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product added to wishlist", item));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable UUID productId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product removed from wishlist", null));
    }
}
