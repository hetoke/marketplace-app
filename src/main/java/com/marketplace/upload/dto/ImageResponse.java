package com.marketplace.upload.dto;

import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import java.time.Instant;
import java.util.UUID;

@lombok.Builder
public record ImageResponse(
        String id,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        EntityType entityType,
        String entityId,
        String uploadedBy,
        Instant createdAt
) {
    public static ImageResponse from(Image image) {
        return ImageResponse.builder()
                .id(image.getId().toString())
                .fileUrl(image.getFileUrl())
                .fileName(image.getFileName())
                .fileSize(image.getFileSize())
                .contentType(image.getContentType())
                .entityType(image.getEntityType())
                .entityId(image.getEntityId().toString())
                .uploadedBy(image.getUploadedBy().toString())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
