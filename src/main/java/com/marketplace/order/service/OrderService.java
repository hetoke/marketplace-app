package com.marketplace.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.cart.model.Cart;
import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.order.dto.CancelOrderRequest;
import com.marketplace.order.dto.OrderItemResponse;
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
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest request) {
        UUID buyerId = UUID.fromString(userId);

        Cart cart = cartRepository.findByUserIdAndStatus(buyerId, Cart.Status.ACTIVE)
                .orElseThrow(() -> new BusinessException("No active cart found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        String shippingAddressJson;
        try {
            shippingAddressJson = new ObjectMapper().writeValueAsString(request.shippingAddress());
        } catch (Exception e) {
            throw new BusinessException("Failed to process shipping address");
        }

        Order order = new Order(buyerId, BigDecimal.ZERO, "USD", shippingAddressJson);
        order = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProductId()));

            if (!product.isActive()) {
                throw new BusinessException("Product '" + product.getName() + "' is no longer available");
            }

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("Insufficient stock for '" + product.getName()
                        + "'. Available: " + product.getStock() + ", requested: " + cartItem.getQuantity());
            }

            product.setStock(product.getStock() - cartItem.getQuantity());
            try {
                productRepository.save(product);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new BusinessException("Product '" + product.getName()
                        + "' is no longer available due to concurrent purchase. Please try again.");
            }

            OrderItem orderItem = new OrderItem(
                    order,
                    product.getId(),
                    product.getName(),
                    null,
                    cartItem.getUnitPrice(),
                    cartItem.getQuantity()
            );
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
            orderItemRepository.save(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        cartItemRepository.deleteByCartId(cart.getId());
        cart.setStatus(Cart.Status.CONVERTED);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, itemResponses);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(String userId) {
        UUID buyerId = UUID.fromString(userId);
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        return orders.stream()
                .map(order -> {
                    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                            .map(OrderItemResponse::from)
                            .toList();
                    return OrderResponse.from(order, items);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userId, UUID orderId) {
        UUID buyerId = UUID.fromString(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Order does not belong to this user");
        }

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items);
    }

    @Transactional
    public OrderResponse updateStatus(String userId, UUID orderId, OrderStatus newStatus) {
        UUID buyerId = UUID.fromString(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Order does not belong to this user");
        }

        if (newStatus == OrderStatus.SHIPPED && order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new BusinessException("Order must be paid before shipping");
        }

        OrderStatus currentStatus = order.getStatus();
        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
        }
        order.setUpdatedAt(Instant.now());
        order = orderRepository.save(order);

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items);
    }

    @Transactional
    public OrderResponse cancelOrder(String userId, UUID orderId, CancelOrderRequest request) {
        UUID buyerId = UUID.fromString(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Order does not belong to this user");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BusinessException("Cannot cancel a paid order. Request a refund first.");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUND_REQUESTED) {
            throw new BusinessException("Cannot cancel an order with a pending refund.");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Order can only be cancelled from PENDING or CONFIRMED status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancelReason(request.reason());
        order.setUpdatedAt(Instant.now());

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + orderItem.getQuantity());
                productRepository.save(product);
            }
        }

        order = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, itemResponses);
    }

    @Transactional
    public OrderResponse requestReturn(String userId, UUID orderId, ReturnRequest request) {
        UUID buyerId = UUID.fromString(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Order does not belong to this user");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Return can only be requested for DELIVERED orders");
        }

        if (order.isReturnRequested()) {
            throw new BusinessException("Return has already been requested for this order");
        }

        order.setReturnRequested(true);
        order.setReturnReason(request.reason());
        order.setReturnRequestedAt(Instant.now());
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setUpdatedAt(Instant.now());
        order = orderRepository.save(order);

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> next == OrderStatus.RETURN_REQUESTED;
            case RETURN_REQUESTED -> next == OrderStatus.RETURNED;
            case CANCELLED, RETURNED -> false;
        };
        if (!valid) {
            throw new BusinessException("Invalid status transition from " + current + " to " + next);
        }
    }
}
