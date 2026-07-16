package com.marketplace.review.controller;

import com.marketplace.review.dto.CreateReviewRequest;
import com.marketplace.review.dto.ReviewResponse;
import com.marketplace.review.dto.UpdateReviewRequest;
import com.marketplace.review.service.ReviewService;
import com.marketplace.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Review created", response));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getUserReviews() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ReviewResponse> reviews = reviewService.getUserReviews(userId);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(@PathVariable UUID reviewId) {
        ReviewResponse response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('BUYER') and @permissionService.isOwnerOfReview(#reviewId)")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review updated", response));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('BUYER') and @permissionService.isOwnerOfReview(#reviewId)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID reviewId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted", null));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId, Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }
}
