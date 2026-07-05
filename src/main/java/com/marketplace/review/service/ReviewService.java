package com.marketplace.review.service;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderItem;
import com.marketplace.order.repository.OrderItemRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.review.dto.CreateReviewRequest;
import com.marketplace.review.dto.ReviewResponse;
import com.marketplace.review.dto.UpdateReviewRequest;
import com.marketplace.review.model.Review;
import com.marketplace.review.repository.ReviewRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse createReview(String userId, CreateReviewRequest request) {
        UUID buyerId = UUID.fromString(userId);

        productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.productId()));

        if (reviewRepository.existsByProductIdAndBuyerId(request.productId(), buyerId)) {
            throw new BusinessException("You have already reviewed this product");
        }

        boolean verifiedPurchase = hasPurchasedProduct(buyerId, request.productId());

        Review review = new Review(
                request.productId(),
                buyerId,
                request.rating(),
                request.comment(),
                verifiedPurchase
        );
        review = reviewRepository.save(review);

        User buyer = userRepository.findById(buyerId).orElse(null);
        String buyerName = buyer != null ? buyer.getDisplayName() : null;

        return ReviewResponse.from(review, buyerName);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(review -> {
                    User buyer = userRepository.findById(review.getBuyerId()).orElse(null);
                    String buyerName = buyer != null ? buyer.getDisplayName() : null;
                    return ReviewResponse.from(review, buyerName);
                });
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User buyer = userRepository.findById(review.getBuyerId()).orElse(null);
        String buyerName = buyer != null ? buyer.getDisplayName() : null;

        return ReviewResponse.from(review, buyerName);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews(String userId) {
        UUID buyerId = UUID.fromString(userId);
        return reviewRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(review -> {
                    User buyer = userRepository.findById(review.getBuyerId()).orElse(null);
                    String buyerName = buyer != null ? buyer.getDisplayName() : null;
                    return ReviewResponse.from(review, buyerName);
                })
                .toList();
    }

    @Transactional
    public ReviewResponse updateReview(String userId, UUID reviewId, UpdateReviewRequest request) {
        UUID buyerId = UUID.fromString(userId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getBuyerId().equals(buyerId)) {
            throw new BusinessException("You can only update your own reviews");
        }

        if (request.rating() != null) {
            review.setRating(request.rating());
        }
        if (request.comment() != null) {
            review.setComment(request.comment());
        }
        review.setUpdatedAt(Instant.now());
        review = reviewRepository.save(review);

        User buyer = userRepository.findById(buyerId).orElse(null);
        String buyerName = buyer != null ? buyer.getDisplayName() : null;

        return ReviewResponse.from(review, buyerName);
    }

    @Transactional
    public void deleteReview(String userId, UUID reviewId) {
        UUID buyerId = UUID.fromString(userId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getBuyerId().equals(buyerId)) {
            throw new BusinessException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Double getProductAverageRating(UUID productId) {
        return reviewRepository.findAverageRatingByProductId(productId).orElse(0.0);
    }

    @Transactional(readOnly = true)
    public Long getProductReviewCount(UUID productId) {
        return reviewRepository.countByProductId(productId);
    }

    private boolean hasPurchasedProduct(UUID buyerId, UUID productId) {
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        for (Order order : orders) {
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                for (OrderItem item : items) {
                    if (item.getProductId().equals(productId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
