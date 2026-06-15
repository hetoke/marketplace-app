package com.marketplace.product.controller;

import com.marketplace.image.dto.ImageRequest;
import com.marketplace.image.dto.ImageResponse;
import com.marketplace.image.model.EntityType;
import com.marketplace.image.service.ImageService;
import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.product.dto.ProductSearchRequest;
import com.marketplace.product.service.ProductService;
import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ImageService imageService;

    public ProductController(ProductService productService, ImageService imageService) {
        this.productService = productService;
        this.imageService = imageService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProductResponse product = productService.createProduct(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", product));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            ProductSearchRequest request) {
        PageResponse<ProductResponse> products = productService.searchProducts(request);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable UUID productId) {
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request) {
        String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProductResponse product = productService.updateProduct(sellerId, productId, request);
        return ResponseEntity.ok(ApiResponse.ok("Product updated", product));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID productId) {
        String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
        productService.deleteProduct(sellerId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<ImageResponse>> addProductImage(
            @PathVariable UUID productId,
            @Valid @RequestBody ImageRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ImageRequest productImageRequest = new ImageRequest(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                EntityType.PRODUCT,
                productId
        );
        ImageResponse image = imageService.saveImage(userId, productImageRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Image added", image));
    }

    @GetMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getProductImages(
            @PathVariable UUID productId) {
        List<ImageResponse> images = imageService.getImages(EntityType.PRODUCT, productId);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        imageService.deleteImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted", null));
    }
}
