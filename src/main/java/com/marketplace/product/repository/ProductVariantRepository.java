package com.marketplace.product.repository;

import com.marketplace.product.model.ProductVariant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stock = v.stock - :quantity WHERE v.id = :variantId AND v.stock >= :quantity")
    int decrementStock(@Param("variantId") UUID variantId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stock = v.stock + :quantity WHERE v.id = :variantId")
    int incrementStock(@Param("variantId") UUID variantId, @Param("quantity") int quantity);
}
