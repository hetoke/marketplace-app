package com.marketplace.review.service;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderItem;
import com.marketplace.order.repository.OrderItemRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductVariant;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.repository.ProductVariantRepository;
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
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         ProductVariantRepository productVariantRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         UserRepository userRepository,
                         CacheManager cacheManager) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public ReviewResponse createReview(String userId, CreateReviewRequest request) {
        UUID buyerId = UUID.fromString(userId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.productId()));

        boolean verifiedPurchase = hasPurchasedProduct(buyerId, request.productId());

        Review review = reviewRepository.findByProductIdAndBuyerId(request.productId(), buyerId)
                .orElse(null);

        if (review != null) {
            review.setRating(request.rating());
            review.setComment(request.comment());
            review.setVariantId(request.variantId());
            review.setUpdatedAt(Instant.now());
        } else {
            review = new Review(
                    request.productId(),
                    buyerId,
                    request.rating(),
                    request.comment(),
                    verifiedPurchase
            );
            review.setVariantId(request.variantId());
        }
        review = reviewRepository.save(review);

        recalculateProductRating(product);

        User buyer = userRepository.findById(buyerId).orElse(null);
        String buyerName = buyer != null ? buyer.getDisplayName() : null;

        return ReviewResponse.from(review, buyerName, resolveVariantAttributes(review.getVariantId()));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(review -> {
                    User buyer = userRepository.findById(review.getBuyerId()).orElse(null);
                    String buyerName = buyer != null ? buyer.getDisplayName() : null;
                    return ReviewResponse.from(review, buyerName, resolveVariantAttributes(review.getVariantId()));
                });
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User buyer = userRepository.findById(review.getBuyerId()).orElse(null);
        String buyerName = buyer != null ? buyer.getDisplayName() : null;

        return ReviewResponse.from(review, buyerName, resolveVariantAttributes(review.getVariantId()));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews(String userId) {
        UUID buyerId = UUID.fromString(userId);
        return reviewRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(review -> {
                    User buyer = userRepository.findById(review.getBuyerId()).orElse(null);
                    String buyerName = buyer != null ? buyer.getDisplayName() : null;
                    return ReviewResponse.from(review, buyerName, resolveVariantAttributes(review.getVariantId()));
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

        productRepository.findById(review.getProductId()).ifPresent(this::recalculateProductRating);

        User buyer = userRepository.findById(buyerId).orElse(null);
        String buyerName = buyer != null ? buyer.getDisplayName() : null;

        return ReviewResponse.from(review, buyerName, resolveVariantAttributes(review.getVariantId()));
    }

    @Transactional
    public void deleteReview(String userId, UUID reviewId) {
        UUID buyerId = UUID.fromString(userId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getBuyerId().equals(buyerId)) {
            throw new BusinessException("You can only delete your own reviews");
        }

        UUID productId = review.getProductId();
        reviewRepository.delete(review);

        productRepository.findById(productId).ifPresent(this::recalculateProductRating);
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

    private void recalculateProductRating(Product product) {
        Double avg = reviewRepository.findAverageRatingByProductId(product.getId()).orElse(0.0);
        Long count = reviewRepository.countByProductId(product.getId());
        product.setAverageRating(Math.round(avg * 10.0) / 10.0);
        product.setReviewCount(count.intValue());
        productRepository.save(product);
        var productByIdCache = cacheManager.getCache("productById");
        if (productByIdCache != null) productByIdCache.evict(product.getId());
    }

    private Map<String, String> resolveVariantAttributes(UUID variantId) {
        if (variantId == null) return null;
        return productVariantRepository.findById(variantId)
                .map(ProductVariant::getAttributes)
                .orElse(null);
    }
}
