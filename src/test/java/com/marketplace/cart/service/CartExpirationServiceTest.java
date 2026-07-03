package com.marketplace.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.cart.model.Cart;
import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartExpirationServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartExpirationService cartExpirationService;

    @Test
    void expireAbandonedCarts_restoresStockForExpiredCarts() {
        UUID userUuid = UUID.randomUUID();
        Cart cart = new Cart(userUuid);
        cart.setId(UUID.randomUUID());

        UUID productId = UUID.randomUUID();
        CartItem item = new CartItem(cart, productId, 3, new BigDecimal("29.99"));
        item.setId(UUID.randomUUID());

        when(cartRepository.findExpiredCarts(eq(Cart.Status.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
        when(productRepository.incrementStock(productId, 3)).thenReturn(1);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartExpirationService.expireAbandonedCarts();

        verify(productRepository).incrementStock(productId, 3);
        assertThat(cart.getStatus()).isEqualTo(Cart.Status.ABANDONED);
    }

    @Test
    void expireAbandonedCarts_handlesMultipleItemsInCart() {
        UUID userUuid = UUID.randomUUID();
        Cart cart = new Cart(userUuid);
        cart.setId(UUID.randomUUID());

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        CartItem item1 = new CartItem(cart, productId1, 2, new BigDecimal("29.99"));
        CartItem item2 = new CartItem(cart, productId2, 5, new BigDecimal("19.99"));

        when(cartRepository.findExpiredCarts(eq(Cart.Status.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item1, item2));
        when(productRepository.incrementStock(productId1, 2)).thenReturn(1);
        when(productRepository.incrementStock(productId2, 5)).thenReturn(1);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartExpirationService.expireAbandonedCarts();

        verify(productRepository).incrementStock(productId1, 2);
        verify(productRepository).incrementStock(productId2, 5);
        assertThat(cart.getStatus()).isEqualTo(Cart.Status.ABANDONED);
    }

    @Test
    void expireAbandonedCarts_handlesNoExpiredCarts() {
        when(cartRepository.findExpiredCarts(eq(Cart.Status.ACTIVE), any(Instant.class)))
                .thenReturn(List.of());

        cartExpirationService.expireAbandonedCarts();

        verify(cartItemRepository, never()).findByCartId(any());
        verify(productRepository, never()).incrementStock(any(), any(int.class));
    }

    @Test
    void expireAbandonedCarts_handlesMultipleExpiredCarts() {
        UUID userUuid1 = UUID.randomUUID();
        Cart cart1 = new Cart(userUuid1);
        cart1.setId(UUID.randomUUID());

        UUID userUuid2 = UUID.randomUUID();
        Cart cart2 = new Cart(userUuid2);
        cart2.setId(UUID.randomUUID());

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        CartItem item1 = new CartItem(cart1, productId1, 1, new BigDecimal("29.99"));
        CartItem item2 = new CartItem(cart2, productId2, 4, new BigDecimal("19.99"));

        when(cartRepository.findExpiredCarts(eq(Cart.Status.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(cart1, cart2));
        when(cartItemRepository.findByCartId(cart1.getId())).thenReturn(List.of(item1));
        when(cartItemRepository.findByCartId(cart2.getId())).thenReturn(List.of(item2));
        when(productRepository.incrementStock(productId1, 1)).thenReturn(1);
        when(productRepository.incrementStock(productId2, 4)).thenReturn(1);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart1, cart2);

        cartExpirationService.expireAbandonedCarts();

        verify(productRepository).incrementStock(productId1, 1);
        verify(productRepository).incrementStock(productId2, 4);
        assertThat(cart1.getStatus()).isEqualTo(Cart.Status.ABANDONED);
        assertThat(cart2.getStatus()).isEqualTo(Cart.Status.ABANDONED);
    }
}
