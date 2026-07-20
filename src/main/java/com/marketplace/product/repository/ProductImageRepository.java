package com.marketplace.product.repository;

import com.marketplace.product.model.ProductImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);
    List<ProductImage> findByProductIdInOrderBySortOrderAsc(List<UUID> productIds);
    void deleteByProductId(UUID productId);
}
