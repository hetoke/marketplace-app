package com.marketplace.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.wishlist.dto.WishlistItemResponse;
import com.marketplace.wishlist.model.WishlistItem;
import com.marketplace.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private Product createTestProduct(UUID sellerId) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSellerId(sellerId);
        product.setName("Test Product");
        product.setSlug("test-product");
        product.setPrice(new BigDecimal("29.99"));
        product.setStock(10);
        product.setActive(true);
        return product;
    }

    @Test
    void getWishlist_returnsItems() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        WishlistItem item = new WishlistItem(userUuid, product.getId());
        item.setId(UUID.randomUUID());

        when(wishlistRepository.findByUserIdOrderByAddedAtDesc(userUuid))
                .thenReturn(List.of(item));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        List<WishlistItemResponse> response = wishlistService.getWishlist(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).productName()).isEqualTo("Test Product");
    }

    @Test
    void getWishlist_returnsEmptyList() {
        UUID userUuid = UUID.fromString(USER_ID);

        when(wishlistRepository.findByUserIdOrderByAddedAtDesc(userUuid))
                .thenReturn(List.of());

        List<WishlistItemResponse> response = wishlistService.getWishlist(USER_ID);

        assertThat(response).isEmpty();
    }

    @Test
    void addToWishlist_addsItem() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        WishlistItem item = new WishlistItem(userUuid, product.getId());
        item.setId(UUID.randomUUID());

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserIdAndProductId(userUuid, product.getId()))
                .thenReturn(false);
        when(wishlistRepository.save(org.mockito.ArgumentMatchers.any(WishlistItem.class)))
                .thenReturn(item);

        WishlistItemResponse response = wishlistService.addToWishlist(USER_ID, product.getId());

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(product.getId().toString());
    }

    @Test
    void addToWishlist_throwsException_whenProductNotFound() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist(USER_ID, productId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addToWishlist_throwsException_whenAlreadyInWishlist() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserIdAndProductId(userUuid, product.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> wishlistService.addToWishlist(USER_ID, product.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in wishlist");
    }

    @Test
    void removeFromWishlist_removesItem() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID productId = UUID.randomUUID();

        when(wishlistRepository.existsByUserIdAndProductId(userUuid, productId))
                .thenReturn(true);

        wishlistService.removeFromWishlist(USER_ID, productId);

        verify(wishlistRepository).deleteByUserIdAndProductId(userUuid, productId);
    }

    @Test
    void removeFromWishlist_throwsException_whenItemNotFound() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID productId = UUID.randomUUID();

        when(wishlistRepository.existsByUserIdAndProductId(userUuid, productId))
                .thenReturn(false);

        assertThatThrownBy(() -> wishlistService.removeFromWishlist(USER_ID, productId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
