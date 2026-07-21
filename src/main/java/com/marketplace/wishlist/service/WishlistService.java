package com.marketplace.wishlist.service;

import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.wishlist.dto.WishlistItemResponse;
import com.marketplace.wishlist.model.WishlistItem;
import com.marketplace.wishlist.repository.WishlistRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String userId) {
        UUID userUuid = UUID.fromString(userId);
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByAddedAtDesc(userUuid);

        return items.stream()
                .map(item -> {
                    String productName = productRepository.findById(item.getProductId())
                            .map(Product::getName)
                            .orElse("Unknown Product");
                    return WishlistItemResponse.from(item, productName);
                })
                .toList();
    }

    @Transactional
    public WishlistItemResponse addToWishlist(String userId, UUID productId) {
        UUID userUuid = UUID.fromString(userId);

        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (wishlistRepository.existsByUserIdAndProductId(userUuid, productId)) {
            throw new BusinessException("Product already in wishlist");
        }

        WishlistItem item = new WishlistItem(userUuid, productId);
        WishlistItem saved = wishlistRepository.save(item);

        String productName = productRepository.findById(productId)
                .map(Product::getName)
                .orElse("Unknown Product");

        return WishlistItemResponse.from(saved, productName);
    }

    @Transactional
    public void removeFromWishlist(String userId, UUID productId) {
        UUID userUuid = UUID.fromString(userId);

        if (!wishlistRepository.existsByUserIdAndProductId(userUuid, productId)) {
            throw new ResourceNotFoundException("Wishlist item", "productId", productId);
        }

        wishlistRepository.deleteByUserIdAndProductId(userUuid, productId);
    }

    @Transactional
    public void clearWishlist(String userId) {
        UUID userUuid = UUID.fromString(userId);
        wishlistRepository.deleteByUserId(userUuid);
    }
}
