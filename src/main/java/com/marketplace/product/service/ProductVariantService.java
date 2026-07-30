package com.marketplace.product.service;

import com.marketplace.product.dto.ProductVariantRequest;
import com.marketplace.product.dto.ProductVariantResponse;
import com.marketplace.product.model.Product;
import com.marketplace.product.model.ProductVariant;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.repository.ProductVariantRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    public ProductVariantService(
        ProductVariantRepository productVariantRepository,
        ProductRepository productRepository
    ) {
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        return productVariantRepository.findByProductIdOrderBySortOrderAsc(productId)
                .stream()
                .map(ProductVariantResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
    public ProductVariantResponse createVariant(String sellerId, UUID productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        if (!product.getSellerId().toString().equals(sellerId)) {
            throw new AccessDeniedException("You can only add variants to your own products");
        }

        String sku = generateSku(product, request.attributes());
        while (productVariantRepository.existsBySku(sku)) {
            sku = generateSku(product, request.attributes()) + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        ProductVariant variant = new ProductVariant(
                product,
                sku,
                request.price(),
                request.stock(),
                request.attributes() != null ? request.attributes() : java.util.Map.of(),
                request.sortOrder() != null ? request.sortOrder() : 0
        );
        productVariantRepository.save(variant);
        recomputeAggregateStock(product);
        return ProductVariantResponse.from(variant);
    }

    @Transactional
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
    public ProductVariantResponse updateVariant(
            String sellerId,
            UUID productId,
            UUID variantId,
            ProductVariantRequest request
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        if (!product.getSellerId().toString().equals(sellerId)) {
            throw new AccessDeniedException("You can only update variants of your own products");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new AccessDeniedException("Variant does not belong to this product");
        }

        variant.setPrice(request.price());
        variant.setStock(request.stock());
        if (request.attributes() != null) {
            variant.setAttributes(request.attributes());
        }
        if (request.sortOrder() != null) {
            variant.setSortOrder(request.sortOrder());
        }
        variant.setUpdatedAt(Instant.now());
        productVariantRepository.save(variant);
        recomputeAggregateStock(product);
        return ProductVariantResponse.from(variant);
    }

    @Transactional
    @CacheEvict(value = {"products", "productById"}, allEntries = true)
    public void deleteVariant(String sellerId, UUID productId, UUID variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        if (!product.getSellerId().toString().equals(sellerId)) {
            throw new AccessDeniedException("You can only delete variants of your own products");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new AccessDeniedException("Variant does not belong to this product");
        }

        productVariantRepository.delete(variant);
        recomputeAggregateStock(product);
    }

    private void recomputeAggregateStock(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        if (variants.isEmpty()) {
            if (product.getStock() == null) {
                product.setStock(0);
            }
        } else {
            int totalStock = variants.stream()
                    .mapToInt(ProductVariant::getStock)
                    .sum();
            product.setStock(totalStock);
        }
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    private String generateSku(Product product, Map<String, String> attributes) {
        String base = product.getSlug();
        if (attributes != null && !attributes.isEmpty()) {
            String attrPart = attributes.values().stream()
                    .map(v -> v.toLowerCase()
                            .replaceAll("[^a-z0-9\\s-]", "")
                            .replaceAll("\\s+", "-")
                            .replaceAll("-+", "-")
                            .replaceAll("^-|-$", ""))
                    .collect(Collectors.joining("-"));
            return base + "-" + attrPart;
        }
        return base;
    }
}
