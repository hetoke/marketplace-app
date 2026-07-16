package com.marketplace.shared.security;

import com.marketplace.cart.model.CartItem;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.notification.model.Notification;
import com.marketplace.notification.repository.NotificationRepository;
import com.marketplace.order.model.Order;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.review.model.Review;
import com.marketplace.review.repository.ReviewRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("permissionService")
public class PermissionService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    private final NotificationRepository notificationRepository;

    public PermissionService(
        ProductRepository productRepository,
        OrderRepository orderRepository,
        PaymentRepository paymentRepository,
        ReviewRepository reviewRepository,
        CartItemRepository cartItemRepository,
        NotificationRepository notificationRepository
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.cartItemRepository = cartItemRepository;
        this.notificationRepository = notificationRepository;
    }

    public boolean isOwnerOfProduct(UUID productId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        return productRepository.findById(productId)
            .map(product -> product.getSellerId().equals(currentUserId))
            .orElse(false);
    }

    public boolean isOwnerOfOrder(UUID orderId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        return orderRepository.findById(orderId)
            .map(order -> order.getBuyerId().equals(currentUserId))
            .orElse(false);
    }

    public boolean isOwnerOfPayment(UUID paymentId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        return paymentRepository.findById(paymentId)
            .flatMap(payment -> orderRepository.findById(payment.getOrderId()))
            .map(order -> order.getBuyerId().equals(currentUserId))
            .orElse(false);
    }

    public boolean isOwnerOfReview(UUID reviewId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        return reviewRepository.findById(reviewId)
            .map(review -> review.getBuyerId().equals(currentUserId))
            .orElse(false);
    }

    public boolean isOwnerOfCartItem(UUID itemId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        return cartItemRepository.findById(itemId)
            .map(item -> item.getCart().getUserId().equals(currentUserId))
            .orElse(false);
    }

    public boolean isOwnerOfNotification(UUID notificationId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        return notificationRepository.findById(notificationId)
            .map(notification -> notification.getUserId().equals(currentUserId))
            .orElse(false);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        String principal = authentication.getName();
        if (principal == null || principal.isBlank()) return null;
        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
