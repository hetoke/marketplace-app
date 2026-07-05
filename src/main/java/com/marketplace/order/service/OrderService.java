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
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.notification.model.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        NotificationService notificationService,
                        PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.paymentRepository = paymentRepository;
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

        Order order = new Order(buyerId, BigDecimal.ZERO, "VND", shippingAddressJson);
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

        notificationService.createNotification(
                buyerId,
                NotificationType.ORDER_UPDATE,
                "Order Placed",
                "Your order #" + order.getId().toString().substring(0, 8).toUpperCase() + " has been placed successfully.",
                order.getId(),
                "ORDER"
        );

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
            if (product != null && product.getStock() <= 5) {
                notificationService.createNotification(
                        product.getSellerId(),
                        NotificationType.LOW_STOCK,
                        "Low Stock Alert",
                        "Product '" + product.getName() + "' has only " + product.getStock() + " items remaining.",
                        product.getId(),
                        "PRODUCT"
                );
            }
        }

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
                    String paymentId = findPaymentId(order.getId());
                    return OrderResponse.from(order, items, paymentId);
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

        String paymentId = findPaymentId(order.getId());
        return OrderResponse.from(order, items, paymentId);
    }

    private String findPaymentId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED
                        || p.getStatus() == Payment.PaymentStatus.REFUND_REQUESTED
                        || p.getStatus() == Payment.PaymentStatus.REFUNDED)
                .findFirst()
                .map(p -> p.getId().toString())
                .orElse(null);
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

        String statusMessage = switch (newStatus) {
            case CONFIRMED -> "Your order has been confirmed.";
            case SHIPPED -> "Your order has been shipped.";
            case DELIVERED -> "Your order has been delivered.";
            case CANCELLED -> "Your order has been cancelled.";
            case RETURN_REQUESTED -> "Your return request has been submitted.";
            case RETURNED -> "Your order has been returned.";
            default -> "Your order status has been updated.";
        };
        notificationService.createNotification(
                buyerId,
                NotificationType.ORDER_UPDATE,
                "Order Status Updated",
                statusMessage,
                order.getId(),
                "ORDER"
        );

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items, findPaymentId(order.getId()));
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
            productRepository.incrementStock(orderItem.getProductId(), orderItem.getQuantity());
        }

        order = orderRepository.save(order);

        notificationService.createNotification(
                buyerId,
                NotificationType.ORDER_UPDATE,
                "Order Cancelled",
                "Your order #" + order.getId().toString().substring(0, 8).toUpperCase() + " has been cancelled.",
                order.getId(),
                "ORDER"
        );

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, itemResponses, findPaymentId(order.getId()));
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

        final Order savedOrder = order;
        UUID orderIdRef = savedOrder.getId();
        paymentRepository.findByOrderIdAndStatus(orderIdRef, Payment.PaymentStatus.COMPLETED)
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.REFUND_REQUESTED);
                    payment.setUpdatedAt(Instant.now());
                    paymentRepository.save(payment);
                    savedOrder.setPaymentStatus(Order.PaymentStatus.REFUND_REQUESTED);
                    orderRepository.save(savedOrder);

                    notificationService.createNotification(
                            buyerId,
                            NotificationType.PAYMENT_UPDATE,
                            "Refund Requested",
                            "Your refund for order #" + orderIdRef.toString().substring(0, 8).toUpperCase() + " has been automatically submitted.",
                            orderIdRef,
                            "ORDER"
                    );
                });

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items, findPaymentId(order.getId()));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> {
                    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                            .map(OrderItemResponse::from)
                            .toList();
                    String paymentId = findPaymentId(order.getId());
                    return OrderResponse.from(order, items, paymentId);
                })
                .toList();
    }

    @Transactional
    public OrderResponse updateStatusAsAdmin(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

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

        notificationService.createNotification(
                order.getBuyerId(),
                NotificationType.ORDER_UPDATE,
                "Order Status Updated",
                "Your order #" + order.getId().toString().substring(0, 8).toUpperCase() + " status has been updated to " + newStatus.name() + ".",
                order.getId(),
                "ORDER"
        );

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, items, findPaymentId(order.getId()));
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
