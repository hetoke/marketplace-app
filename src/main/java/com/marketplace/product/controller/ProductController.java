package com.marketplace.product.controller;

import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.dto.ProductSearchRequest;
import com.marketplace.product.service.ProductService;
import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
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
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductRequest request
    ) {
        String sellerId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        System.out.println(request);
        ProductResponse product = productService.createProduct(
            sellerId,
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.ok("Product created", product)
        );
    }

    @GetMapping
    public ResponseEntity<
        ApiResponse<PageResponse<ProductResponse>>
    > searchProducts(ProductSearchRequest request) {
        PageResponse<ProductResponse> products = productService.searchProducts(
            request
        );
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
        @PathVariable UUID productId
    ) {
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER') and @permissionService.isOwnerOfProduct(#productId)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
        @PathVariable UUID productId,
        @Valid @RequestBody ProductRequest request
    ) {
        String sellerId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        ProductResponse product = productService.updateProduct(
            sellerId,
            productId,
            request
        );
        return ResponseEntity.ok(ApiResponse.ok("Product updated", product));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER') and @permissionService.isOwnerOfProduct(#productId)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
        @PathVariable UUID productId
    ) {
        String sellerId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        productService.deleteProduct(sellerId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER') and @permissionService.isOwnerOfProduct(#productId)")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
        @PathVariable UUID productId,
        @PathVariable UUID imageId
    ) {
        String sellerId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        productService.deleteProductImage(sellerId, productId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Product image deleted", null));
    }
}
