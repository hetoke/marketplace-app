package com.marketplace.upload.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.upload.dto.UploadResponse;
import com.marketplace.upload.service.UploadService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @GetMapping("/api/v1/users/avatar/upload-url")
    public ResponseEntity<ApiResponse<UploadResponse>> requestUserAvatar() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UploadResponse response = uploadService.requestUserUpload(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Upload session created", response));
    }

    @GetMapping("/api/v1/products/{productId}/images/upload-url")
    public ResponseEntity<ApiResponse<UploadResponse>> requestProductImages(
            @PathVariable UUID productId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UploadResponse response = uploadService.requestProductUpload(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Upload session created", response));
    }
}
