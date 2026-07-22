package com.marketplace.admin.service;

import com.marketplace.admin.dto.AdminProductResponse;
import com.marketplace.admin.dto.ProductStatusUpdateRequest;
import com.marketplace.admin.model.AdminActionLog;
import com.marketplace.admin.model.AdminActionLog.Action;
import com.marketplace.admin.repository.AdminActionLogRepository;
import com.marketplace.shared.dto.PageResponse;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final AdminActionLogRepository actionLogRepository;

    public AdminProductService(ProductRepository productRepository, AdminActionLogRepository actionLogRepository) {
        this.productRepository = productRepository;
        this.actionLogRepository = actionLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> getProducts(int page, int size, UUID categoryId, Boolean active, String search) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.searchAdmin(search, categoryId, active, pageRequest);

        return new PageResponse<>(
                products.getContent().stream().map(AdminProductResponse::from).toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
        );
    }

    @Transactional
    @CacheEvict(value = {"products", "productById", "analyticsRevenue", "analyticsOrders", "analyticsUsers", "analyticsProducts"}, allEntries = true)
    public AdminProductResponse updateProductStatus(UUID productId, boolean active, UUID adminId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        boolean oldActive = product.isActive();
        product.setActive(active);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);

        String details = String.format("{\"oldActive\":%b,\"newActive\":%b}", oldActive, active);
        AdminActionLog logEntry = new AdminActionLog(adminId, Action.PRODUCT_STATUS_CHANGE, "PRODUCT", productId, details);
        actionLogRepository.save(logEntry);

        return AdminProductResponse.from(product);
    }
}
