package com.marketplace.upload.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.upload.dto.AvatarUploadRequest;
import com.marketplace.upload.dto.ProductImageUploadRequest;
import com.marketplace.upload.dto.UploadResponse;
import com.marketplace.upload.service.UploadService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/api/v1/users/avatar")
    public ResponseEntity<ApiResponse<UploadResponse>> requestUserAvatar(
            @Valid @RequestBody AvatarUploadRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UploadResponse response = uploadService.requestUserUpload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Upload session created", response));
    }

    @PostMapping("/api/v1/products/{productId}/images")
    public ResponseEntity<ApiResponse<UploadResponse>> requestProductImages(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductImageUploadRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UploadResponse response = uploadService.requestProductUpload(userId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Upload session created", response));
    }
}
