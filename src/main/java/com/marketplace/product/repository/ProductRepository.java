package com.marketplace.product.repository;

import com.marketplace.product.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrue(UUID categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR :query IS NULL) AND " +
           "(p.category.id = :categoryId OR :categoryId IS NULL) AND " +
           "(p.price >= :minPrice OR :minPrice IS NULL) AND " +
           "(p.price <= :maxPrice OR :maxPrice IS NULL) AND " +
           "p.active = true")
    Page<Product> search(
            @Param("query") String query,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable);
}
