package com.marketplace.product.controller;

import com.marketplace.product.dto.ProductVariantRequest;
import com.marketplace.product.dto.ProductVariantResponse;
import com.marketplace.product.service.ProductVariantService;
import com.marketplace.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/products/{productId}/variants")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariants(
            @PathVariable UUID productId
    ) {
        List<ProductVariantResponse> variants = productVariantService.getVariants(productId);
        return ResponseEntity.ok(ApiResponse.ok(variants));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER') and @permissionService.isOwnerOfProduct(#productId)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProductVariantResponse variant = productVariantService.createVariant(sellerId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Variant created", variant));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasRole('SELLER') and @permissionService.isOwnerOfProduct(#productId)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProductVariantResponse variant = productVariantService.updateVariant(sellerId, productId, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Variant updated", variant));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('SELLER') and @permissionService.isOwnerOfProduct(#productId)")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId
    ) {
        String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
        productVariantService.deleteVariant(sellerId, productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Variant deleted", null));
    }
}
