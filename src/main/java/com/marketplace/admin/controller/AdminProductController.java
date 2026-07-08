package com.marketplace.admin.controller;

import com.marketplace.admin.dto.AdminProductResponse;
import com.marketplace.admin.dto.ProductStatusUpdateRequest;
import com.marketplace.admin.service.AdminProductService;
import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.shared.dto.PageResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {
        PageResponse<AdminProductResponse> products = adminProductService.getProducts(page, size, categoryId, active, search);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @PutMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<AdminProductResponse>> updateProductStatus(
            @PathVariable UUID productId,
            @RequestBody ProductStatusUpdateRequest request,
            Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminProductResponse product = adminProductService.updateProductStatus(productId, request.active(), adminId);
        return ResponseEntity.ok(ApiResponse.ok("Product status updated", product));
    }
}
