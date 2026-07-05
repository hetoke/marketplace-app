package com.marketplace.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

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

    private Review createTestReview(UUID productId, UUID buyerId) {
        Review review = new Review(productId, buyerId, 5, "Great product!", true);
        review.setId(UUID.randomUUID());
        return review;
    }

    @Test
    void createReview_createsReviewSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        Review review = createTestReview(product.getId(), userUuid);
        User buyer = new User();
        buyer.setId(userUuid);
        buyer.setDisplayName("Test Buyer");

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndBuyerId(product.getId(), userUuid))
                .thenReturn(false);
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(userUuid))
                .thenReturn(List.of());
        when(reviewRepository.save(any(Review.class)))
                .thenReturn(review);
        when(userRepository.findById(userUuid))
                .thenReturn(Optional.of(buyer));

        CreateReviewRequest request = new CreateReviewRequest(product.getId(), 5, "Great product!");
        ReviewResponse response = reviewService.createReview(USER_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great product!");
        assertThat(response.buyerName()).isEqualTo("Test Buyer");
    }

    @Test
    void createReview_throwsException_whenProductNotFound() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        CreateReviewRequest request = new CreateReviewRequest(productId, 5, "Great!");
        assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReview_throwsException_whenAlreadyReviewed() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndBuyerId(product.getId(), userUuid))
                .thenReturn(true);

        CreateReviewRequest request = new CreateReviewRequest(product.getId(), 5, "Great!");
        assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void updateReview_updatesReviewSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        Review review = createTestReview(product.getId(), userUuid);
        User buyer = new User();
        buyer.setId(userUuid);
        buyer.setDisplayName("Test Buyer");

        when(reviewRepository.findById(review.getId()))
                .thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class)))
                .thenReturn(review);
        when(userRepository.findById(userUuid))
                .thenReturn(Optional.of(buyer));

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated comment");
        ReviewResponse response = reviewService.updateReview(USER_ID, review.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo(4);
    }

    @Test
    void updateReview_throwsException_whenNotOwner() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID otherUserId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        Review review = createTestReview(product.getId(), otherUserId);

        when(reviewRepository.findById(review.getId()))
                .thenReturn(Optional.of(review));

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated");
        assertThatThrownBy(() -> reviewService.updateReview(USER_ID, review.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own reviews");
    }

    @Test
    void deleteReview_deletesReviewSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        Review review = createTestReview(product.getId(), userUuid);

        when(reviewRepository.findById(review.getId()))
                .thenReturn(Optional.of(review));

        reviewService.deleteReview(USER_ID, review.getId());

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_throwsException_whenNotOwner() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID otherUserId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Product product = createTestProduct(sellerId);
        Review review = createTestReview(product.getId(), otherUserId);

        when(reviewRepository.findById(review.getId()))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(USER_ID, review.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own reviews");
    }

    @Test
    void getProductAverageRating_returnsAverage() {
        UUID productId = UUID.randomUUID();

        when(reviewRepository.findAverageRatingByProductId(productId))
                .thenReturn(Optional.of(4.5));

        Double average = reviewService.getProductAverageRating(productId);

        assertThat(average).isEqualTo(4.5);
    }

    @Test
    void getProductReviewCount_returnsCount() {
        UUID productId = UUID.randomUUID();

        when(reviewRepository.countByProductId(productId))
                .thenReturn(10L);

        Long count = reviewService.getProductReviewCount(productId);

        assertThat(count).isEqualTo(10L);
    }
}
