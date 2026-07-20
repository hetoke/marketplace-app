package com.marketplace.product.controller;

import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.service.ProductService;
import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.shared.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getMyProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        String sellerId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        PageResponse<ProductResponse> products = productService.getSellerProducts(
            sellerId,
            page,
            size
        );
        return ResponseEntity.ok(ApiResponse.ok(products));
    }
}
