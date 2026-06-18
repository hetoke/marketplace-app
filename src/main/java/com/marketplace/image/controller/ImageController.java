package com.marketplace.image.controller;

import com.marketplace.image.dto.ImageResponse;
import com.marketplace.image.model.EntityType;
import com.marketplace.image.service.ImageService;
import com.marketplace.shared.dto.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getImages(
            @RequestParam EntityType entityType,
            @RequestParam UUID entityId) {
        List<ImageResponse> images = imageService.getImages(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable UUID imageId) {
        String userId = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        imageService.deleteImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted", null));
    }
}
