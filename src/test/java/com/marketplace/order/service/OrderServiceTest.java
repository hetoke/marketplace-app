package com.marketplace.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.cart.model.Cart;
import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.order.dto.CancelOrderRequest;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.dto.ReturnRequest;
import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderItem;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderItemRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_USER_ID = "22222222-2222-2222-2222-222222222222";

    private Cart cart;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        UUID userUuid = UUID.fromString(USER_ID);
        cart = new Cart(userUuid);
        cart.setId(UUID.randomUUID());

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setPrice(new BigDecimal("29.99"));
        product.setStock(10);
        product.setActive(true);

        cartItem = new CartItem(cart, product.getId(), 2, product.getPrice());
        cartItem.setId(UUID.randomUUID());
    }

    private PlaceOrderRequest createPlaceOrderRequest() {
        return new PlaceOrderRequest(
                new PlaceOrderRequest.ShippingAddress(
                        "123 Main St", "Springfield", "IL", "62701", "US"
                )
        );
    }

    @Test
    void placeOrder_createsOrderSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order savedOrder = new Order(userUuid, new BigDecimal("59.98"), "USD", "{}");
        savedOrder.setId(UUID.randomUUID());

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.findByOrderId(savedOrder.getId())).thenReturn(List.of());

        OrderResponse response = orderService.placeOrder(USER_ID, createPlaceOrderRequest());

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(product.getStock()).isEqualTo(8);
        verify(cartItemRepository).deleteByCartId(cart.getId());
    }

    @Test
    void placeOrder_throwsException_whenCartNotFound() {
        UUID userUuid = UUID.fromString(USER_ID);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, createPlaceOrderRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No active cart found");
    }

    @Test
    void placeOrder_throwsException_whenCartIsEmpty() {
        UUID userUuid = UUID.fromString(USER_ID);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, createPlaceOrderRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void placeOrder_throwsException_whenInsufficientStock() {
        UUID userUuid = UUID.fromString(USER_ID);
        product.setStock(1);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, createPlaceOrderRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void placeOrder_throwsException_whenProductNotActive() {
        UUID userUuid = UUID.fromString(USER_ID);
        product.setActive(false);

        when(cartRepository.findByUserIdAndStatus(userUuid, Cart.Status.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, createPlaceOrderRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void getOrders_returnsOrdersForUser() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(userUuid))
                .thenReturn(List.of(order));
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of());

        List<OrderResponse> orders = orderService.getOrders(USER_ID);

        assertThat(orders).hasSize(1);
    }

    @Test
    void getOrder_returnsOrderForUser() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of());

        OrderResponse response = orderService.getOrder(USER_ID, order.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(order.getId().toString());
    }

    @Test
    void getOrder_throwsException_whenOrderNotBelongToUser() {
        UUID otherUserUuid = UUID.fromString(OTHER_USER_ID);
        Order order = new Order(otherUserUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(USER_ID, order.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void cancelOrder_cancelsSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);

        OrderItem item = new OrderItem(order, product.getId(), "Test Product", null, BigDecimal.TEN, 2);
        item.setId(UUID.randomUUID());

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(item), List.of());
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(USER_ID, order.getId(),
                new CancelOrderRequest("Changed my mind"));

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(product.getStock()).isEqualTo(12);
    }

    @Test
    void cancelOrder_throwsException_whenNotCancellable() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, order.getId(),
                new CancelOrderRequest("Reason")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("can only be cancelled");
    }

    @Test
    void requestReturn_requestsSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of());

        OrderResponse response = orderService.requestReturn(USER_ID, order.getId(),
                new ReturnRequest("Defective product"));

        assertThat(response.status()).isEqualTo("RETURN_REQUESTED");
        assertThat(response.returnRequested()).isTrue();
    }

    @Test
    void requestReturn_throwsException_whenNotDelivered() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.requestReturn(USER_ID, order.getId(),
                new ReturnRequest("Reason")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    void updateStatus_transitionsPENDINGtoCONFIRMED() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of());

        OrderResponse response = orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void updateStatus_throwsException_whenInvalidTransition() {
        UUID userUuid = UUID.fromString(USER_ID);
        Order order = new Order(userUuid, BigDecimal.TEN, "USD", "{}");
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(order.getId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), OrderStatus.DELIVERED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
