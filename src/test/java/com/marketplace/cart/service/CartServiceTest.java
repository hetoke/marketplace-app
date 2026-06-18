package com.marketplace.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_USER_ID = "22222222-2222-2222-2222-222222222222";

    private Cart createTestCart(UUID userId) {
        Cart cart = new Cart(userId);
        cart.setId(UUID.randomUUID());
        return cart;
    }

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
    void getCart_createsNewCart_whenNoActiveCartExists() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.itemCount()).isZero();
    }

    @Test
    void getCart_returnsExistingCart_whenActiveCartExists() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(cart.getId().toString());
    }

    @Test
    void addItem_createsNewItem_whenProductNotInCart() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        CartResponse response = cartService.addItem(USER_ID, product.getId(), 2);

        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_increasesQuantity_whenProductAlreadyInCart() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);

        CartItem existingItem = new CartItem(cart, product.getId(), 2, product.getPrice());
        existingItem.setId(UUID.randomUUID());

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        CartResponse response = cartService.addItem(USER_ID, product.getId(), 3);

        assertThat(response).isNotNull();
        assertThat(existingItem.getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_throwsException_whenProductNotFound() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID productId = UUID.randomUUID();

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(USER_ID, productId, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItem_throwsException_whenProductNotActive() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        product.setActive(false);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, product.getId(), 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void addItem_throwsException_whenInsufficientStock() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        product.setStock(3);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, product.getId(), 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void updateQuantity_updatesItemQuantity() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        CartItem item = new CartItem(cart, product.getId(), 2, product.getPrice());
        item.setId(UUID.randomUUID());

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        CartResponse response = cartService.updateQuantity(USER_ID, item.getId(), 5);

        assertThat(response).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity_throwsException_whenCartItemNotFound() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID itemId = UUID.randomUUID();

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, itemId, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateQuantity_throwsException_whenItemBelongsToDifferentCart() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        Cart otherCart = createTestCart(UUID.fromString(OTHER_USER_ID));
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        CartItem item = new CartItem(otherCart, product.getId(), 2, product.getPrice());
        item.setId(UUID.randomUUID());

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, item.getId(), 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void removeItem_removesItemFromCart() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        CartItem item = new CartItem(cart, product.getId(), 2, product.getPrice());
        item.setId(UUID.randomUUID());

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        CartResponse response = cartService.removeItem(USER_ID, item.getId());

        assertThat(response).isNotNull();
        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeItem_throwsException_whenItemBelongsToDifferentCart() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);
        Cart otherCart = createTestCart(UUID.fromString(OTHER_USER_ID));
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        CartItem item = new CartItem(otherCart, product.getId(), 2, product.getPrice());
        item.setId(UUID.randomUUID());

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeItem(USER_ID, item.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void clearCart_removesAllItems() {
        UUID userUuid = UUID.fromString(USER_ID);
        Cart cart = createTestCart(userUuid);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));

        cartService.clearCart(USER_ID);

        verify(cartItemRepository).deleteByCartId(cart.getId());
    }
}
