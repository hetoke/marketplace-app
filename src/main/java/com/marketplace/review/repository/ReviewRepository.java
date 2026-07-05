package com.marketplace.review.repository;

import com.marketplace.review.model.Review;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    List<Review> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<Review> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    Optional<Review> findByProductIdAndBuyerId(UUID productId, UUID buyerId);

    boolean existsByProductIdAndBuyerId(UUID productId, UUID buyerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Optional<Double> findAverageRatingByProductId(@Param("productId") UUID productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId")
    Long countByProductId(@Param("productId") UUID productId);
}
