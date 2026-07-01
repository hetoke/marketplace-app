package com.marketplace.upload.dto;

import com.marketplace.upload.model.EntityType;
import com.marketplace.upload.model.Image;
import java.time.Instant;
import java.util.UUID;

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
        return new ImageResponse(
                image.getId().toString(),
                image.getFileUrl(),
                image.getFileName(),
                image.getFileSize(),
                image.getContentType(),
                image.getEntityType(),
                image.getEntityId().toString(),
                image.getUploadedBy().toString(),
                image.getCreatedAt()
        );
    }
}
